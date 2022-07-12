package com.infinite.inventory.strategy;

import java.util.TreeSet;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;

public interface CostingStrategy extends Runnable {
	
	/**
	 * put existing costing records before calculation for new inventory transaction
	 * 
	 * @param existingCosting existing costing records, sorted by accounting date, ascending
	 */
	void init(TreeSet<Costing> existingCosting);
	
	void appendTransaction(MaterialTransaction record);
	
}
