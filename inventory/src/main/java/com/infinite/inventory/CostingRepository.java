package com.infinite.inventory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.SortCostingByValidFrom;

@Component
public class CostingRepository {
	
	Map<String, TreeSet<Costing>> costingsByProductCorrelationId = new HashMap<>(); //product correlation id --> Costing[]
	
	List<Consumer<Costing>> subscribers = new LinkedList<>();
	
	public Map<String, TreeSet<Costing>> findByProductCorellationIds(String[] productCorellationIds) {
		Map<String, TreeSet<Costing>> result = new HashMap<>();
		for (String corellationId : productCorellationIds) {
			if (costingsByProductCorrelationId.containsKey(corellationId))
				result.put(corellationId, costingsByProductCorrelationId.get(corellationId));
		}
		
		return result;
	}

	public TreeSet<Costing> findByProduct(Product product) {
		if (!costingsByProductCorrelationId.containsKey(product.getCorrelationId()))
			return new TreeSet<>();
		
		return costingsByProductCorrelationId.get(product.getCorrelationId());
	}

	public void save(Costing newCosting) {
		
		if (StringUtils.isBlank(newCosting.getId()))
			newCosting.setId(UUID.randomUUID().toString());
		
		String productCorrelationId = newCosting.getProduct().getCorrelationId();
		
		if (costingsByProductCorrelationId.containsKey(productCorrelationId)) {
			TreeSet<Costing> costings = costingsByProductCorrelationId.get(productCorrelationId);
			costings.add(newCosting);
			
			notify(newCosting);
			
			return;
		}
		
		TreeSet<Costing> costings = new TreeSet<>(new SortCostingByValidFrom());
		costings.add(newCosting);
		costingsByProductCorrelationId.put(newCosting.getProduct().getCorrelationId(), costings);
		
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

	public Map<String, TreeSet<Costing>> findAll() {
		return costingsByProductCorrelationId;
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
	public Map<String, TreeSet<Costing>> search(String where, String[] productCorellationIds) {
		// TODO Auto-generated method stub
		return new HashMap<String, TreeSet<Costing>>();
	}
	
}
