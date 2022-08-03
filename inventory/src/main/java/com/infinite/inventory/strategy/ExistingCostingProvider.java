package com.infinite.inventory.strategy;

import java.util.LinkedList;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

public interface ExistingCostingProvider {
	
	LinkedList<Costing> get(Product product);

}
