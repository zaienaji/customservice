package com.infinite.inventory.strategy;

import java.util.Optional;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

public interface CostingStrategy extends Runnable {
	void start();
	
	void appendTransaction(MaterialTransaction record);
	
	void pushTransaction(MaterialTransaction record);

	Optional<String> updateTransaction(MaterialTransaction record);
	
}
