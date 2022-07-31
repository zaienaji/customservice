package com.infinite.inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.MovementType;

@Component
public class MaterialTransactionRepository {
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private final List<Consumer<MaterialTransaction>> materialTransctionChangeSubscriber = new LinkedList<Consumer<MaterialTransaction>>();

	private final HashMap<String, MaterialTransaction> cacheByMovementOutId = new HashMap<>();
	
	public void save(MaterialTransaction materialTransaction) {
		
		materialTransaction.setError(StringUtils.isNotBlank(materialTransaction.getCostingErrorMessage()));
		
		if (StringUtils.isBlank(materialTransaction.getId())) {
			materialTransaction.setId(UUID.randomUUID().toString());
			insertToDb(materialTransaction);
		} else {
			updateDb(materialTransaction);
		}
		
		if (StringUtils.isNotBlank(materialTransaction.getMovementOutCorrelationId()))
			cacheByMovementOutId.put(materialTransaction.getMovementOutCorrelationId(), materialTransaction);
		
		if (materialTransaction.getMovementType()==MovementType.MovementIn && !materialTransaction.isError())
			cacheByMovementOutId.remove(materialTransaction.getMovementOutCorrelationId());
		
		notifyMaterialTransctionChanged(materialTransaction);
	}

	private void updateDb(MaterialTransaction materialTransaction) {
		jdbcTemplate.update("UPDATE public.materialtransaction "
				+ " SET correlation_id=?, product_correlation_id=?, product_valuation_type=?, "
				+ " movement_type=?, movement_qty=?, acquisition_cost=?, movement_date=?,"
				+ " costing_status=?, costing_error_message=?, movement_out_correlation_id=?,"
				+ " customer_shipment_correlation_id=?, iserror=?"
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
		    materialTransaction.isError(),
		    materialTransaction.getId());
		
	}

	private void insertToDb(MaterialTransaction materialTransaction) {
		jdbcTemplate.update(
		    "INSERT INTO public.materialtransaction"
		    + " (id, correlation_id, product_correlation_id, product_valuation_type, movement_type, movement_qty,"
		    + "  acquisition_cost, movement_date, costing_status, costing_error_message,"
		    + "  movement_out_correlation_id, customer_shipment_correlation_id, iserror)"
		    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
		    materialTransaction.getCustomerShipmentCorrelationId(),
		    materialTransaction.isError()
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

	public MaterialTransaction[] findByCorellationIds(String[] materialTransactionCorellationIds) {
		
		String param = Arrays.stream(materialTransactionCorellationIds).collect(Collectors.joining("', '", "'", "'"));
		String query = String.format("select * from materialtransaction where correlation_id in (%s)", param);		
		
		List<MaterialTransaction> result = jdbcTemplate.query(query, new MaterialCostingRowMapper());
		MaterialTransaction[] resultArr = result.toArray(new MaterialTransaction[result.size()]);
		return resultArr;
	}

	public MaterialTransaction[] findAll() {
		
		String query = "select * from materialtransaction";		
		List<MaterialTransaction> result = jdbcTemplate.query(query, new MaterialCostingRowMapper());
				
		return result.toArray(new MaterialTransaction[result.size()]);
	}

	public MaterialTransaction[] findAllError() {		
		String query = "select * from materialtransaction where iserror=true";		
		List<MaterialTransaction> result = jdbcTemplate.query(query, new MaterialCostingRowMapper());
				
		return result.toArray(new MaterialTransaction[result.size()]);
	}

}
