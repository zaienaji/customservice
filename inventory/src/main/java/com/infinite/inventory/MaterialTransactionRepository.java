package com.infinite.inventory;

import static com.infinite.inventory.sharedkernel.CostingStatus.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

@Component
public class MaterialTransactionRepository {
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private final List<Consumer<MaterialTransaction>> materialTransctionChangeSubscriber = new LinkedList<Consumer<MaterialTransaction>>();
	
	private final HashMap<String, MaterialTransaction> cacheByCorellationId = new HashMap<>();
	private final HashMap<String, MaterialTransaction> cacheByMovementOutId = new HashMap<>();
	
	public void save(MaterialTransaction materialTransaction) {
		if (StringUtils.isBlank(materialTransaction.getId())) {
			materialTransaction.setId(UUID.randomUUID().toString());
			insertToDb(materialTransaction);
		} else {
			updateDb(materialTransaction);
		}
		
		cacheByCorellationId.put(materialTransaction.getCorrelationId(), materialTransaction);
		
		//TODO how to remove cache when all related movement in/out have been calculated?
		/*
		 * if we save movement in transaction, with success state, then remove pair movement in out from cache
		 * if
		 */
		if (StringUtils.isNotBlank(materialTransaction.getMovementOutCorrelationId()))
			cacheByMovementOutId.put(materialTransaction.getMovementOutCorrelationId(), materialTransaction);
		
		notifyMaterialTransctionChanged(materialTransaction);
	}

	private void updateDb(MaterialTransaction materialTransaction) {
		jdbcTemplate.update("UPDATE public.materialtransaction "
				+ " SET correlation_id=?, product_correlation_id=?, product_valuation_type=?, "
				+ " movement_type=?, movement_qty=?, acquisition_cost=?, movement_date=?,"
				+ " costing_status=?, costing_error_message=?, movement_out_correlation_id=?,"
				+ " customer_shipment_correlation_id=?"
				+ " WHERE id=?", 
				materialTransaction.getCorrelationId(),
		    materialTransaction.getProduct().getCorrelationId(),
		    materialTransaction.getProduct().getValuationType().toString(),
		    materialTransaction.getMovementType().toString(),
		    materialTransaction.getMovementQuantity(),
		    materialTransaction.getAcquisitionCost(),
		    materialTransaction.getMovementDate(),
		    materialTransaction.getCostingStatus().toString(),
		    materialTransaction.getCostingErrorMessage(),
		    materialTransaction.getMovementOutCorrelationId(), 
		    materialTransaction.getCustomerShipmentCorrelationId(),
		    materialTransaction.getId());
		
	}

	private void insertToDb(MaterialTransaction materialTransaction) {
		jdbcTemplate.update(
		    "INSERT INTO public.materialtransaction"
		    + " (id, correlation_id, product_correlation_id, product_valuation_type, movement_type, movement_qty,"
		    + "  acquisition_cost, movement_date, costing_status, costing_error_message,"
		    + "  movement_out_correlation_id, customer_shipment_correlation_id)"
		    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
		    materialTransaction.getId(), 
		    materialTransaction.getCorrelationId(),
		    materialTransaction.getProduct().getCorrelationId(),
		    materialTransaction.getProduct().getValuationType().toString(),
		    materialTransaction.getMovementType().toString(),
		    materialTransaction.getMovementQuantity(),
		    materialTransaction.getAcquisitionCost(),
		    materialTransaction.getMovementDate(),
		    materialTransaction.getCostingStatus().toString(),
		    materialTransaction.getCostingErrorMessage(),
		    materialTransaction.getMovementOutCorrelationId(), 
		    materialTransaction.getCustomerShipmentCorrelationId()
		);
	}

	private void notifyMaterialTransctionChanged(MaterialTransaction materialTransaction) {
		for (Consumer<MaterialTransaction> subscriber : materialTransctionChangeSubscriber)
			subscriber.accept(materialTransaction);
	}
	
	public void addSubscriber(Consumer<MaterialTransaction> subscriber) {
		materialTransctionChangeSubscriber.add(subscriber);
	}
	
	public Optional<MaterialTransaction> findByMovementOutCorrelationId(String movementOutCorrelationId) {
		if (cacheByMovementOutId.containsKey(movementOutCorrelationId))
			return Optional.of(cacheByMovementOutId.get(movementOutCorrelationId));
		
		return Optional.empty();
	}

	//TODO remove cache, use database instead
	public MaterialTransaction[] findByCorellationIds(String[] materialTransactionCorellationIds) {
		List<MaterialTransaction> result = new ArrayList<>();
		
		for (String corellationId : materialTransactionCorellationIds) {
			if (cacheByCorellationId.containsKey(corellationId))
				result.add(cacheByCorellationId.get(corellationId));
		}
		
		MaterialTransaction[] resultArr = result.toArray(new MaterialTransaction[result.size()]);
		return resultArr;
	}

	public MaterialTransaction[] findAll() {
		
		String query = "select * from materialtransaction";		
		List<MaterialTransaction> result = jdbcTemplate.query(query, new MaterialCostingRowMapper());
				
		return result.toArray(new MaterialTransaction[result.size()]);
	}

	//TODO remove cache, use database instead
	public MaterialTransaction[] findAllError() {
		List<MaterialTransaction> result = new LinkedList<>();
		
		for (MaterialTransaction materialTransaction : cacheByCorellationId.values()) {
			if (materialTransaction.getCostingStatus()==Error)
				result.add(materialTransaction);
		}

		MaterialTransaction[] resultArr = result.toArray(new MaterialTransaction[result.size()]);
		return resultArr;
	}

}
