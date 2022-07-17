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
				pendingTransaction.setCostingErrorMessage("negative movement quantity is allowed for PhysicalInventory only");
				
				materialTransactionRepository.save(pendingTransaction);
				pushTransaction(pendingTransaction);
				
				break;
			}
						
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
				
				case CustomerReturn:
				case MovementIn:
					handleMovementIn(pendingTransaction);
					break;
					
				default:
					throw new IllegalArgumentException("Unexpected value: " + pendingTransaction.getMovementType());
			}
		}
		
		isRun.set(false);
	}

	private void handlePhysicalInventoryIn(MaterialTransaction pendingTransaction) {
		if (costings.size()==0) {
			pendingTransaction.setCostingStatus(Error);
			pendingTransaction.setCostingErrorMessage("no current costing information avaialable");
			
			pushTransaction(pendingTransaction);
			return;
		}
		
		//TODO review implementation
		if (isNonZero(pendingTransaction.getAcquisitionCost())) {
			pendingTransaction.setCostingStatus(Calculated);
			materialTransactionRepository.save(pendingTransaction);
			
			Costing recentCost = costings.getLast();
			Costing newCosting = new Costing(pendingTransaction.getProduct());
			newCosting.setTotalQty(recentCost.getTotalQty().add(pendingTransaction.getMovementQuantity()));
			newCosting.setTotalCost(recentCost.getTotalCost().add(pendingTransaction.getAcquisitionCost()));
			newCosting.setUnitCost(newCosting.getTotalCost().divide(newCosting.getTotalQty(), 2, RoundingMode.HALF_DOWN));
			newCosting.setValidFrom(pendingTransaction.getMovementDate());
			costings.addLast(newCosting);
			costingRepository.save(newCosting);
		} else {
			Costing recentCost = costings.getLast();
			pendingTransaction.setAcquisitionCost(recentCost.getUnitCost().multiply(pendingTransaction.getMovementQuantity()));
			pendingTransaction.setCostingStatus(Calculated);
			materialTransactionRepository.save(pendingTransaction);
			
			recentCost.setValidTo(pendingTransaction.getMovementDate());
			costingRepository.save(recentCost);
			
			Costing newCosting = new Costing(pendingTransaction.getProduct());
			newCosting.setTotalQty(recentCost.getTotalQty().add(pendingTransaction.getMovementQuantity()));
			newCosting.setTotalCost(recentCost.getTotalCost().add(pendingTransaction.getAcquisitionCost()));
			newCosting.setUnitCost(newCosting.getTotalCost().divide(newCosting.getTotalQty(), 2, RoundingMode.HALF_DOWN));
			newCosting.setValidFrom(pendingTransaction.getMovementDate());
			costings.addLast(newCosting);
			costingRepository.save(newCosting);
		}
		
	}

	

	private boolean isNonZero(BigDecimal number) {
		return number != null && number.compareTo(BigDecimal.ZERO)!=0;
	}

	private void handlePhysicalInventoryOut(MaterialTransaction pendingTransaction) {
		if (costings.size()==0) {
			pendingTransaction.setCostingStatus(Error);
			pendingTransaction.setCostingErrorMessage("no current costing information avaialable");
			
			pushTransaction(pendingTransaction);
			return;
		}
		
		//TODO handle when acquisition cost exists.
		
		Costing recentCost = costings.getLast();
		pendingTransaction.setAcquisitionCost(recentCost.getUnitCost().multiply(pendingTransaction.getMovementQuantity()));
		pendingTransaction.setCostingStatus(Calculated);
		materialTransactionRepository.save(pendingTransaction);
		
		recentCost.setTotalQty(recentCost.getTotalQty().subtract(pendingTransaction.getMovementQuantity()));
		costingRepository.save(recentCost);
	}

	private void handleMovementIn(MaterialTransaction pendingTransaction) {
		
		String inoutCorellationId = pendingTransaction.getMovementOutCorrelationId();
		MaterialTransaction matchedMaterialTransaction = materialTransactionRepository.findByInOutCorellationId(inoutCorellationId);
		
		if (matchedMaterialTransaction==null) {
			appendTransaction(pendingTransaction);
			return;
		}
		
		BigDecimal transactionCost = matchedMaterialTransaction.getAcquisitionCost();
		pendingTransaction.setAcquisitionCost(transactionCost);
		pendingTransaction.setCostingStatus(Calculated);
		
		materialTransactionRepository.save(pendingTransaction);
		
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
