package com.infinite.inventory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class CostingRepository {
	
	Map<Product, TreeSet<Costing>> cache = new HashMap<>();
	Map<String, Product> corellationIdToProductMapper = new HashMap<>();
	
	List<Consumer<Costing>> subscribers = new LinkedList<>();
	

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

	public void save(Costing newCosting) {
		corellationIdToProductMapper.put(newCosting.getCorrelationId(), newCosting.getProduct());
		
		if (cache.containsKey(newCosting.getProduct())) {
			TreeSet<Costing> costings = cache.get(newCosting.getProduct());
			costings.add(newCosting);
			
			notify(newCosting);
			
			return;
		}
		
		TreeSet<Costing> costings = new TreeSet<>();
		costings.add(newCosting);
		cache.put(newCosting.getProduct(), costings);
		
		notify(newCosting);
	}

	private void notify(Costing newCosting) {
		for (Consumer<Costing> subscriber : subscribers) {
			subscriber.accept(newCosting);
		}
	}
	
	public void addSubscriber(Consumer<Costing> subscriber) {
		subscribers.add(subscriber);
	}
	
}
