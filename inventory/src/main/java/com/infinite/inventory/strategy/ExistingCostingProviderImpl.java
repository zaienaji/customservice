package com.infinite.inventory.strategy;

import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class ExistingCostingProviderImpl implements ExistingCostingProvider {

	@Override
	public TreeSet<Costing> get(Product product) {
		
		//TODO implement get existing material costing
		//try to not tightly coupled with Openbravo
		return new TreeSet<Costing>();
	}

}
