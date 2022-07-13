package com.infinite.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

public class CostingRepository {
	
	Map<Product, TreeSet<Costing>> cache = new HashMap<>();
	Map<String, Product> corellationIdToProductMapper = new HashMap<>();
	

	public Map<Product, TreeSet<Costing>> findByProductCorellationIds(String[] productCorellationIds) {
		Map<Product, TreeSet<Costing>> result = new HashMap<>();
		
		for (String corellationId : productCorellationIds) {
			if (!corellationIdToProductMapper.containsKey(corellationId))
				continue;
			
			Product product = corellationIdToProductMapper.get(corellationId);
			TreeSet<Costing> costings = cache.get(product);
			result.put(product, costings);
		}
		
		return result;
	}

	public TreeSet<Costing> findByProduct(Product product) {
		if (!cache.containsKey(product))
			return new TreeSet<>();
		
		return cache.get(product);
	}

}
