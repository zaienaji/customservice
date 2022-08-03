package com.infinite.inventory.strategy;

import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class ExistingCostingProviderImpl implements ExistingCostingProvider {
	
	@Autowired
	private CostingRepository repository;

	@Override
	public LinkedList<Costing> get(Product product) {
		
		return repository.findByProduct(product);
	}

}
