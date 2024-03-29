package com.infinite.inventory.strategy;

import static com.infinite.inventory.sharedkernel.CostingStatus.Calculated;
import static com.infinite.inventory.sharedkernel.CostingStatus.Error;
import static com.infinite.inventory.util.Util.isNullOrZero;
import static com.infinite.inventory.util.Util.isZeroOrNegative;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.OperationsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.ThreadPool;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.util.Util;


public class MovingAverageStrategy implements CostingStrategy {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final Costing costing;
	
	private final static LocalDateTime doomsDay = LocalDateTime.of(9999, 12, 31, 0, 0);
	
	private ReentrantLock lock = new ReentrantLock(); 
	private final LinkedList<MaterialTransaction> pendingTransactions = new LinkedList<>(); //TODO fix reconstruct of pendingTransactions when addTop operation happened in the past.
	
	private final MaterialTransactionRepository materialTransactionRepository;
	private final CostingRepository costingRepository;
	
	private final Semaphore semaphore = new Semaphore(1);
	
	private final ThreadPool threadPool;
	
	public MovingAverageStrategy(ThreadPool threadPool, MaterialTransactionRepository materialTransactionRepository,
			CostingRepository costingRepository, Optional<Costing> existingCosting, Product product) {
		super();
		
		this.threadPool = threadPool;
		this.materialTransactionRepository = materialTransactionRepository;
		this.costingRepository = costingRepository;
		this.costing = existingCosting.isPresent()? existingCosting.get() : new Costing(product);
	}

	public void start() {
		
		if (semaphore.availablePermits()==0)
			return;
		
		try {
			semaphore.acquire();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		threadPool.execute(this);
	}

	public void appendTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			materialTransactionRepository.save(record);
			pendingTransactions.addLast(record);
			start();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			lock.unlock();
		}
		
		
	}
	
	@Override
	public void pushTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			materialTransactionRepository.save(record);
			pendingTransactions.addFirst(record);
			start();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			lock.unlock();
		}
		
		
	}

	@Override
	public void run() {
		while(true) {
			if (pendingTransactions.size()==0)
				break;
			
			MaterialTransaction pendingTransaction = getPendingTransaction();
			
			try {
				logger.info("processing transaction correlation id: "+pendingTransaction.getCorrelationId());
				handlePendingTransaction(pendingTransaction);
				
			} catch (Exception e) {
				logger.error(
						String.format("Transaction with correlation id %s with product correlation id %s throw error message: %s", 
								pendingTransaction.getCorrelationId(), pendingTransaction.getProduct().getCorrelationId(), e.getMessage()));
				
				pendingTransaction.setCostingErrorMessage(e.getMessage());
				pendingTransaction.setCostingStatus(Error);
				
				materialTransactionRepository.save(pendingTransaction);
				pushTransaction(pendingTransaction);
				
				break;
			}
		}
		
		semaphore.release();
	}

	private void handlePendingTransaction(MaterialTransaction pendingTransaction) throws OperationsException {
		
		if (isZeroOrNegative(pendingTransaction.getMovementQuantity()))
			throw new OperationsException("negative movement quantity is not allowed");
		
		switch (pendingTransaction.getMovementType()) {
		case PhysicalInventoryIn:
			handlePhysicalInventoryIn(pendingTransaction);
			break;

		case PhysicalInventoryOut:
			handlePhysicalInventoryOut(pendingTransaction);
			break;

		case VendorReturn:
		case MovementOut:
		case CustomerShipment:
			makeSureSufficientQuantity(pendingTransaction);
			handleCustomerShipment(pendingTransaction);
			break;

		case VendorReceipt:
			handleVendorReceipt(pendingTransaction);
			break;
			
		case MovementIn:
			handleMovementIn(pendingTransaction);
			break;

		case CustomerReturn:
		default:
			throw new IllegalArgumentException("Unexpected value: " + pendingTransaction.getMovementType());
		}
		
	}

	private void makeSureSufficientQuantity(MaterialTransaction pendingTransaction) throws OperationsException {
		BigDecimal quantityOnHand = costing.getTotalQty().subtract(pendingTransaction.getMovementQuantity());
		if(Util.isNegative(quantityOnHand))
			throw new OperationsException("not sufficient quantity");
		
	}

	private void handlePhysicalInventoryIn(MaterialTransaction pendingTransaction) throws OperationsException {
		
		BigDecimal acquisitionCost = pendingTransaction.getAcquisitionCost();
		if (isNullOrZero(acquisitionCost)) {
			if (Util.isZero(costing.getUnitCost()))
				throw new OperationsException("transaction has no acquisition cost, and no recent cost, hence can not determine unit cost");
			
			acquisitionCost = costing.getUnitCost().multiply(pendingTransaction.getMovementQuantity());
			pendingTransaction.setAcquisitionCost(acquisitionCost);
		}
		
		pendingTransaction.setCostingStatus(Calculated);
		pendingTransaction.setCostingErrorMessage(null);
		pendingTransaction.setError(false);
		materialTransactionRepository.save(pendingTransaction);
		
		costing.setTotalQty(costing.getTotalQty().add(pendingTransaction.getMovementQuantity()));
		costing.setTotalCost(costing.getTotalCost().add(acquisitionCost));
		costing.setUnitCost(costing.getTotalCost().divide(costing.getTotalQty(), 2, RoundingMode.HALF_DOWN));
		costing.setValidFrom(pendingTransaction.getMovementDate());
		
		costingRepository.save(costing);
	}

	private void handlePhysicalInventoryOut(MaterialTransaction pendingTransaction) throws OperationsException {
		
		if (Util.isNullOrZero(costing.getUnitCost()))
			throw new OperationsException("physical inventory out can not find recent cost, hence can not determine unit cost");
			
		BigDecimal acquisitionCost = pendingTransaction.getAcquisitionCost();
		if (isNullOrZero(acquisitionCost)) {
			acquisitionCost = costing.getUnitCost().multiply(pendingTransaction.getMovementQuantity());
			pendingTransaction.setAcquisitionCost(costing.getUnitCost().multiply(pendingTransaction.getMovementQuantity()));
		}
		
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
		
		costing.setTotalQty(costing.getTotalQty().subtract(pendingTransaction.getMovementQuantity()));
		costing.setTotalCost(costing.getTotalCost().subtract(acquisitionCost));
		costingRepository.save(costing);
	}

	private void handleMovementIn(MaterialTransaction pendingTransaction) throws OperationsException {
		
		String inoutCorellationId = pendingTransaction.getMovementOutCorrelationId();
		Optional<MaterialTransaction> matchedMaterialTransaction = materialTransactionRepository.findByMovementOutCorrelationId(inoutCorellationId);
		
		if (matchedMaterialTransaction.isEmpty())
			throw new OperationsException("can not find matched movement out transaction");
		
		BigDecimal transactionCost = matchedMaterialTransaction.get().getAcquisitionCost();
		pendingTransaction.setAcquisitionCost(transactionCost);
		
		handleVendorReceipt(pendingTransaction);
	}

	private void handleVendorReceipt(MaterialTransaction pendingTransaction) throws OperationsException {
		
		if (isNullOrZero(pendingTransaction.getAcquisitionCost()))
			throw new OperationsException("vendor receipt must have acquisition cost");
		
		BigDecimal totalCost = costing.getTotalCost().add(pendingTransaction.getAcquisitionCost());
		BigDecimal totalQty = costing.getTotalQty().add(pendingTransaction.getMovementQuantity());
		BigDecimal unitCost = totalCost.divide(totalQty, 2, RoundingMode.HALF_DOWN);
		LocalDateTime accountingDate = pendingTransaction.getMovementDate();
		
		costing.setTotalCost(totalCost);
		costing.setTotalQty(totalQty);
		costing.setUnitCost(unitCost);
		costing.setValidFrom(accountingDate);
		costing.setValidTo(doomsDay);
		costingRepository.save(costing);
		
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
	}
	
	private void handleCustomerShipment(MaterialTransaction pendingTransaction) {
		
		BigDecimal currentQty = costing.getTotalQty().subtract(pendingTransaction.getMovementQuantity());
		costing.setTotalQty(currentQty);
		
		BigDecimal currentTotalCost = pendingTransaction.getMovementQuantity().multiply(costing.getUnitCost()); 
		costing.setTotalCost(costing.getTotalCost().subtract(currentTotalCost));
		
		pendingTransaction.setAcquisitionCost(costing.getUnitCost().multiply(pendingTransaction.getMovementQuantity()));
		pendingTransaction.setCostingStatus(Calculated);
		
		materialTransactionRepository.save(pendingTransaction);
		costingRepository.save(costing);
	}

	private MaterialTransaction getPendingTransaction() {
		
		lock.lock();
		try {
			return pendingTransactions.removeFirst();
		}
		finally {
			lock.unlock();
		}
		
	}

	@Override
	public Optional<String> updateTransaction(MaterialTransaction record) {
		
		if (record.getCostingStatus()==Calculated)
			return Optional.of("can not update material transaction with status complete for correlation id "+record.getCorrelationId());
		
		materialTransactionRepository.save(record);
		
		int index = pendingTransactions.indexOf(record);
		if (index == -1)
			return Optional.of("can not find active material transaction with correlation id "+record.getCorrelationId());
		
		pendingTransactions.set(index, record);
		
		try {
			start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return Optional.empty();
	}

}
