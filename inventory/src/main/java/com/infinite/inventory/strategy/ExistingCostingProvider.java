package com.infinite.inventory.strategy;

import java.util.Optional;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

public interface ExistingCostingProvider {
	
	Optional<Costing> get(Product product);

}
