package com.infinite.inventory.sharedkernel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Costing {
	
	private String id;
	private String correlationId;
	private Product product;
	
	private BigDecimal totalQty = BigDecimal.ZERO;
	private BigDecimal unitCost = BigDecimal.ZERO;
	private BigDecimal totalCost = BigDecimal.ZERO;
	private LocalDateTime validFrom = LocalDateTime.of(1970, 1, 1, 0, 0);
	private LocalDateTime validTo = LocalDateTime.of(9999, 12, 31, 0, 0);

	public Costing(Product product) {
		super();
		this.product = product;
	}
}
