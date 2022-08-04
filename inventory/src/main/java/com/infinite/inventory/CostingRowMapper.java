package com.infinite.inventory;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.ValuationType;

public class CostingRowMapper implements RowMapper<Costing> {

	@Override
	public Costing mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		Product product = new Product();
		product.setCorrelationId(rs.getString("product_correlation_id"));
		product.setValuationType(ValuationType.valueOf(rs.getString("product_valuation_type")));
		
		Costing result = new Costing(null);
		result.setProduct(product);
		
		result.setId(rs.getString("id"));
		result.setCorrelationId(rs.getString("correlation_id"));
		
		result.setTotalQty(rs.getBigDecimal("total_qty"));
		result.setUnitCost(rs.getBigDecimal("unit_cost"));
		result.setTotalCost(rs.getBigDecimal("total_cost"));
		
		result.setValidFrom(rs.getTimestamp("valid_from").toLocalDateTime());
		result.setValidTo(rs.getTimestamp("valid_to").toLocalDateTime());
		
		result.setExpired(rs.getBoolean("isexpired"));
		
		return result;
	}

}
