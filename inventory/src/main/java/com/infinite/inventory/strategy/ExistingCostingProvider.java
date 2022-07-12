package com.infinite.inventory.strategy;

import java.util.TreeSet;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

public interface ExistingCostingProvider {
	
	TreeSet<Costing> get(Product product);

}
