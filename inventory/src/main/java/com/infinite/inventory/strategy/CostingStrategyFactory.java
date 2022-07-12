package com.infinite.inventory.strategy;

import java.util.HashMap;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.ValuationType;

@Component
public class CostingStrategyFactory {
	
	private final HashMap<Product, CostingStrategy> cache = new HashMap<>();
	
	@Autowired
	private ExistingCostingProvider existingCostingProvider;
	
	@Autowired
	private MaterialTransactionRepository materialTransactionRepository;

	public CostingStrategy get(Product product) {
		if (cache.containsKey(product))
			return cache.get(product);
		
		CostingStrategy result = get(product.getValuationType());
		TreeSet<Costing> existingCosting = existingCostingProvider.get(product);
		result.init(existingCosting);
		
		cache.put(product, result);
		
		return result;
		
	}
	
	public CostingStrategy get(ValuationType valuationType) {

		switch (valuationType) {

		case MovingAverage:
			return new MovingAverageStrategy(materialTransactionRepository);

		case FIFO:
		case Standard:
		default:
			throw new IllegalArgumentException("not supported valuation type " + valuationType);
		}

	}

}
