package com.infinite.inventory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class CostingRepository {
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	Map<String, Costing> cache = new HashMap<>(); //key is product correlation id
	
	List<Consumer<Costing>> subscribers = new LinkedList<>();
	
	public Map<String, Costing> findByProductCorellationIds(String[] productCorellationIds) {
		Map<String, Costing> result = new HashMap<>();
		for (String corellationId : productCorellationIds) {
			if (cache.containsKey(corellationId))
				result.put(corellationId, cache.get(corellationId));
		}
		
		return result;
	}

	public Optional<Costing> findByProduct(Product product) {
		String sqlQuery = String.format("select * from costing where product_correlation_id = '%s'", product.getCorrelationId());
		
		try {
			Costing result = jdbcTemplate.queryForObject(sqlQuery, new CostingRowMapper());
			return Optional.of(result);
			
		} catch (Exception e) {
			
		}
		
		return Optional.empty();
	}

	public void save(Costing newCosting) {
		String productCorrelationId = newCosting.getProduct().getCorrelationId();
		cache.put(productCorrelationId, newCosting);
		
		if (StringUtils.isBlank(newCosting.getId())) {
			newCosting.setId(UUID.randomUUID().toString());
			insertIntoDd(newCosting);
		} else {
			updateDb(newCosting);
		}
		
		notify(newCosting);
	}

	private void updateDb(Costing costing) {
		
		String query = "UPDATE public.costing "
				+ "SET correlation_id=?, "
				+ "product_correlation_id=?, product_valuation_type=?, "
				+ "total_qty=?, unit_cost=?, total_cost=?, "
				+ "valid_from=?, valid_to=?, isexpired=? "
				+ "WHERE id=?";
		System.out.println(query);
		jdbcTemplate.update(
				"UPDATE public.costing SET "
						+ "correlation_id=?, "
						+ "product_correlation_id=?, product_valuation_type=?, "
						+ "total_qty=?, unit_cost=?, total_cost=?, "
						+ "valid_from=?, valid_to=?, isexpired=? "
						+ "WHERE id=?",
				costing.getCorrelationId(),
				costing.getProduct().getCorrelationId(),
				costing.getProduct().getValuationType().toString(),
				costing.getTotalQty(),
				costing.getUnitCost(),
				costing.getTotalCost(),
				costing.getValidFrom(),
				costing.getValidTo(),
				costing.isExpired(),
				costing.getId());

	}

	private void insertIntoDd(Costing costing) {
		jdbcTemplate.update(
				"INSERT INTO public.costing " + "(id, correlation_id, "
						+ " product_correlation_id, product_valuation_type, total_qty, unit_cost, total_cost, "
						+ " valid_from, valid_to, isexpired) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				costing.getId(), 
				costing.getCorrelationId(), 
				costing.getProduct().getCorrelationId(),
				costing.getProduct().getValuationType().toString(), 
				costing.getTotalQty(), 
				costing.getUnitCost(),
				costing.getTotalCost(), 
				costing.getValidFrom(), 
				costing.getValidTo(), 
				costing.isExpired());

	}

	private void notify(Costing newCosting) {
		for (Consumer<Costing> subscriber : subscribers) {
			subscriber.accept(newCosting);
		}
	}
	
	public void addSubscriber(Consumer<Costing> subscriber) {
		subscribers.add(subscriber);
	}

	public Map<String, Costing> findAll() {
		return cache;
	}
}
