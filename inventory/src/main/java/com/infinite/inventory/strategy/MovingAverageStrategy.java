package com.infinite.inventory.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.MovementType;

import static com.infinite.inventory.sharedkernel.CostingStatus.*;
import static com.infinite.inventory.util.Util.*;

public class MovingAverageStrategy implements CostingStrategy {
	
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
			System.out.println("processing transaction id: "+pendingTransaction.getId());
			
			if (isNegative(pendingTransaction.getMovementQuantity())) {
				pendingTransaction.setCostingStatus(Error);
				pendingTransaction.setCostingErrorMessage("movement quantity is negative");
				
				materialTransactionRepository.save(pendingTransaction);
				pushTransaction(pendingTransaction);
				
				break;
			}
			
			if (pendingTransaction.getMovementType()==MovementType.VendorReceipt && isZero(pendingTransaction.getAcquisitionCost())) {
				pendingTransaction.setCostingStatus(Error);
				pendingTransaction.setCostingErrorMessage("movement type is vendor receipt but acquisition cost is zero");
				
				materialTransactionRepository.save(pendingTransaction);
				pushTransaction(pendingTransaction);
				
				break;
			}
			
			switch (pendingTransaction.getMovementType()) {
				case MovementOut:
				case CustomerShipment:
					handleCustomerShipment(pendingTransaction);
					break;
					
				case PhysicalInventory:
				case VendorReceipt:
					handleVendorReceipt(pendingTransaction);
					break;
					
				case MovementIn:
					handleMovementIn(pendingTransaction);
					break;
					
				default:
					throw new IllegalArgumentException("Unexpected value: " + pendingTransaction.getMovementType());
			}
		}
		
		isRun.set(false);
	}

	private void handleMovementIn(MaterialTransaction pendingTransaction) {
		
		String inoutCorellationId = pendingTransaction.getMovementInOutCorrelationId();
		MaterialTransaction matchedMaterialTransaction = materialTransactionRepository.findByInOutCorellationId(inoutCorellationId);
		
		if (matchedMaterialTransaction==null) {
			appendTransaction(pendingTransaction);
			return;
		}
		
		BigDecimal transactionCost = matchedMaterialTransaction.getAcquisitionCost();
		pendingTransaction.setAcquisitionCost(transactionCost);
		pendingTransaction.setCostingStatus(Calculated);
		
		materialTransactionRepository.save(pendingTransaction);
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
