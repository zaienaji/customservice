package com.infinite.inventory.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.OperationsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;

import static com.infinite.inventory.sharedkernel.CostingStatus.*;
import static com.infinite.inventory.util.Util.*;


public class MovingAverageStrategy implements CostingStrategy {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final static LocalDateTime doomsDay = LocalDateTime.of(9999, 12, 31, 0, 0);

	private final LinkedList<Costing> costings = new LinkedList<>();
	
	private ReentrantLock lock = new ReentrantLock(); 
	private final LinkedList<MaterialTransaction> pendingTransactions = new LinkedList<>();
	
	private final MaterialTransactionRepository materialTransactionRepository;
	private final CostingRepository costingRepository;
	
	private AtomicBoolean isRun = new AtomicBoolean(false);
	
	public MovingAverageStrategy(MaterialTransactionRepository materialTransactionRepository,
			CostingRepository costingRepository) {
		super();
		this.materialTransactionRepository = materialTransactionRepository;
		this.costingRepository = costingRepository;
	}

	public void init(TreeSet<Costing> existingCosting) {
		existingCosting.stream().forEach(c -> costings.addLast(c));
		
		start();
	}

	private void start() {
		if (isRun.get())
			return;
		
		isRun.set(true);
		
		Thread thread = new Thread(this);
		thread.start();
	}

	public void appendTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			pendingTransactions.addLast(record);
		}
		finally {
			lock.unlock();
		}
		
		start();
	}
	
	private void pushTransaction(MaterialTransaction record) {
		lock.lock();
		try {
			pendingTransactions.addFirst(record);
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
			logger.info("processing transaction correlation id: "+pendingTransaction.getCorrelationId());
			
			if (pendingTransaction.getCostingStatus()==Error)
				break;
			
			try {
				handlePendingTransaction(pendingTransaction);
			} catch (Exception e) {
				logger.error(
						String.format("Transaction with correlation id %s with product correlation id %s throw error message: %s", 
								pendingTransaction.getCorrelationId(), pendingTransaction.getProduct().getCorrelationId(), e.getMessage()));
				
				pendingTransaction.setCostingErrorMessage(e.getMessage());
				pendingTransaction.setCostingStatus(Error);
				
				materialTransactionRepository.save(pendingTransaction);
				pushTransaction(pendingTransaction);
			}
		}
		
		isRun.set(false);
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
			if (costings.size()==0)
				throw new OperationsException("transaction has no acquisition cost, and no recent cost, hence can not determine unit cost");
			
			Costing recentCost = costings.getLast();
			acquisitionCost = recentCost.getUnitCost().multiply(pendingTransaction.getMovementQuantity());
			pendingTransaction.setAcquisitionCost(acquisitionCost);
		}
		
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
		
		BigDecimal totalCost = acquisitionCost;
		BigDecimal totalQuantity = pendingTransaction.getMovementQuantity();
		BigDecimal unitCost = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_DOWN);
		
		if (costings.size()>0) {
			Costing recentCost = costings.getLast();
			recentCost.setValidTo(pendingTransaction.getMovementDate());
			
			costingRepository.save(recentCost);
			
			totalCost = totalCost.add(recentCost.getTotalQty());
			totalQuantity = totalQuantity.add(recentCost.getTotalQty());
			unitCost = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_DOWN);
		}
		
		Costing newCosting = new Costing(pendingTransaction.getProduct());
		newCosting.setTotalQty(totalQuantity);
		newCosting.setTotalCost(totalCost);
		newCosting.setUnitCost(unitCost);
		newCosting.setValidFrom(pendingTransaction.getMovementDate());
		costings.addLast(newCosting);
		
		costingRepository.save(newCosting);
	}

	private void handlePhysicalInventoryOut(MaterialTransaction pendingTransaction) throws OperationsException {
		
		if (costings.size()==0)
			throw new OperationsException("physical inventory out can not find recent cost, hence can not determine unit cost");
		
		Costing recentCost = costings.getLast();
			
		BigDecimal acquisitionCost = pendingTransaction.getAcquisitionCost();
		if (isNullOrZero(acquisitionCost)) {
			acquisitionCost = recentCost.getUnitCost().multiply(pendingTransaction.getMovementQuantity());
			pendingTransaction.setAcquisitionCost(recentCost.getUnitCost().multiply(pendingTransaction.getMovementQuantity()));
		}
		
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
		
		recentCost.setTotalQty(recentCost.getTotalQty().subtract(pendingTransaction.getMovementQuantity()));
		recentCost.setTotalCost(recentCost.getTotalCost().subtract(acquisitionCost));
		costingRepository.save(recentCost);
	}

	private void handleMovementIn(MaterialTransaction pendingTransaction) {
		
		String inoutCorellationId = pendingTransaction.getMovementOutCorrelationId();
		Optional<MaterialTransaction> matchedMaterialTransaction = materialTransactionRepository.findByMovementOutCorrelationId(inoutCorellationId);
		
		if (matchedMaterialTransaction.isEmpty()) {
			pendingTransaction.setCostingStatus(Error);
			pendingTransaction.setCostingErrorMessage("can not find matched movement out transaction");
			materialTransactionRepository.save(pendingTransaction);
			
			appendTransaction(pendingTransaction);
			
			return;
		}
		
		BigDecimal transactionCost = matchedMaterialTransaction.get().getAcquisitionCost();
		pendingTransaction.setAcquisitionCost(transactionCost);
		
		handleVendorReceipt(pendingTransaction);
	}

	private void handleVendorReceipt(MaterialTransaction pendingTransaction) {
		Costing costing = null;
		if (costings.size()>0)
			costing = costings.getLast();
		else
			costing = new Costing(pendingTransaction.getProduct());
		
		BigDecimal totalCost = costing.getTotalCost().add(pendingTransaction.getAcquisitionCost());
		BigDecimal totalQty = costing.getTotalQty().add(pendingTransaction.getMovementQuantity());
		BigDecimal unitCost = totalCost.divide(totalQty, 2, RoundingMode.HALF_DOWN);
		LocalDateTime accountingDate = pendingTransaction.getMovementDate();
		
		if (costings.size()>0) {
			Costing recentCosting = costings.getLast();
			recentCosting.setValidTo(accountingDate);
			costingRepository.save(recentCosting);
		}
		
		Costing newCosting = new Costing(pendingTransaction.getProduct());
		newCosting.setTotalCost(totalCost);
		newCosting.setTotalQty(totalQty);
		newCosting.setUnitCost(unitCost);
		newCosting.setValidFrom(accountingDate);
		newCosting.setValidTo(doomsDay);
		costings.addLast(newCosting);
		costingRepository.save(newCosting);
		
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
	}

	private void handleCustomerShipment(MaterialTransaction pendingTransaction) {
		Costing costing = costings.getLast();
		
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

}
