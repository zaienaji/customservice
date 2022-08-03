package com.infinite.inventory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class CostingRepository {
	
	Map<String, LinkedList<Costing>> cache = new HashMap<>(); //product correlation id --> Costing[]
	
	List<Consumer<Costing>> subscribers = new LinkedList<>();
	
	public Map<String, LinkedList<Costing>> findByProductCorellationIds(String[] productCorellationIds) {
		Map<String, LinkedList<Costing>> result = new HashMap<>();
		for (String corellationId : productCorellationIds) {
			if (cache.containsKey(corellationId))
				result.put(corellationId, cache.get(corellationId));
		}
		
		return result;
	}

	public LinkedList<Costing> findByProduct(Product product) {
		if (!cache.containsKey(product.getCorrelationId()))
			return new LinkedList<>();
		
		return cache.get(product.getCorrelationId());
	}

	public void save(Costing newCosting) {
		String productCorrelationId = newCosting.getProduct().getCorrelationId();
		
		if (StringUtils.isBlank(newCosting.getId())) {
			newCosting.setId(UUID.randomUUID().toString());
			
			LinkedList<Costing> costingChain = cache.containsKey(productCorrelationId) ? cache.get(productCorrelationId) : new LinkedList<>(); 
			costingChain.addLast(newCosting);
			cache.put(productCorrelationId, costingChain);
		} else {
			LinkedList<Costing> costingChain = cache.get(productCorrelationId);
			costingChain.removeLast();
			costingChain.addLast(newCosting);
		}
		
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

	public Map<String, LinkedList<Costing>> findAll() {
		return cache;
	}

	public Costing find(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * @param where SQL where clause
	 * @param productCorellationIds additional where clause, if exists, return only costing with this product correlation id(s) 
	 * @return
	 */
	public Map<String, LinkedList<Costing>> search(String where, String[] productCorellationIds) {
		// TODO Auto-generated method stub
		return new HashMap<String, LinkedList<Costing>>();
	}
	
}
