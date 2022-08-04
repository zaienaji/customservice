package com.infinite.inventory.strategy;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

public interface CostingStrategy extends Runnable {
	void start();
	
	void appendTransaction(MaterialTransaction record);
	
}
