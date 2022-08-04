package com.infinite.inventory;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.ValuationType;

public class ProductRowMapper implements RowMapper<Product> {

	@Override
	public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		Product result = new Product();
		result.setCorrelationId(rs.getString("product_correlation_id"));
		result.setValuationType(ValuationType.valueOf(rs.getString("product_valuation_type")));
		
		return result;
	}

}
