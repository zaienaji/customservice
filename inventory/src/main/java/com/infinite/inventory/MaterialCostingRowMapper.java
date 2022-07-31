package com.infinite.inventory;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.infinite.inventory.sharedkernel.CostingStatus;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.MovementType;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.ValuationType;

public class MaterialCostingRowMapper implements RowMapper<MaterialTransaction> {

	@Override
	public MaterialTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
		MaterialTransaction result = new MaterialTransaction();
		
		result.setId(rs.getString("id"));
		result.setCorrelationId(rs.getString("correlation_id"));
		
		Product product = new Product();
		product.setCorrelationId(rs.getString("product_correlation_id"));
		product.setValuationType(ValuationType.valueOf(rs.getString("product_valuation_type")));
		result.setProduct(product);
		
		result.setMovementType(MovementType.valueOf(rs.getString("movement_type")));
		result.setMovementQuantity(rs.getBigDecimal("movement_qty"));
		result.setAcquisitionCost(rs.getBigDecimal("acquisition_cost"));
		result.setMovementDate(rs.getTimestamp("movement_date").toLocalDateTime());
		result.setCostingStatus(CostingStatus.valueOf(rs.getString("costing_status")));
		
		result.setCostingErrorMessage(rs.getString("costing_error_message"));
		result.setMovementOutCorrelationId(rs.getString("movement_out_correlation_id"));
		result.setCustomerShipmentCorrelationId(rs.getString("customer_shipment_correlation_id"));
		
		result.setError(rs.getBoolean("iserror"));
		
		return result;
	}

}
