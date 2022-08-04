package com.infinite.inventory.strategy;

import static com.infinite.inventory.sharedkernel.CostingStatus.Calculated;
import static com.infinite.inventory.sharedkernel.CostingStatus.Error;
import static com.infinite.inventory.util.Util.isNegative;
import static com.infinite.inventory.util.Util.isNullOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.OperationsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.util.Util;


public class MovingAverageStrategy implements CostingStrategy {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final Costing costing;
	
	private final static LocalDateTime doomsDay = LocalDateTime.of(9999, 12, 31, 0, 0);
	
	private ReentrantLock lock = new ReentrantLock(); 
	private final LinkedList<MaterialTransaction> pendingTransactions = new LinkedList<>();
	
	private final MaterialTransactionRepository materialTransactionRepository;
	private final CostingRepository costingRepository;
	
	private AtomicBoolean isWorkerAlive = new AtomicBoolean(false);
	
	private Thread worker;
	
	public MovingAverageStrategy(MaterialTransactionRepository materialTransactionRepository,
			CostingRepository costingRepository, Optional<Costing> existingCosting, Product product) {
		super();
		this.materialTransactionRepository = materialTransactionRepository;
		this.costingRepository = costingRepository;
		this.costing = existingCosting.isPresent()? existingCosting.get() : new Costing(product);
	}

	public void start() {
		if (isWorkerAlive.get())
			return;
		
		isWorkerAlive.set(true);
		
		if (this.worker==null || !this.worker.isAlive()) {
			this.worker = new Thread(this);
			this.worker.start();
		}			
	}

	public void appendTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			materialTransactionRepository.save(record);
			pendingTransactions.addLast(record);
		}
		finally {
			lock.unlock();
		}
		
		start();
	}
	
	@Override
	public void pushTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			materialTransactionRepository.save(record);
			pendingTransactions.addFirst(record);
		}
		finally {
			lock.unlock();
		}
		
		start();
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
		
		isWorkerAlive.set(false);
	}

	private void handlePendingTransaction(MaterialTransaction pendingTransaction) throws OperationsException {
		
		if (isNegative(pendingTransaction.getMovementQuantity()))
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
	public void updateTransaction(MaterialTransaction record) {
		materialTransactionRepository.save(record);
		start();
	}

}
