package com.infinite.inventory.sharedkernel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MaterialTransaction {
	
	private String id;
	private String correlationId;
	private Product product;
	private MovementType movementType;
	private BigDecimal movementQuantity;
	private BigDecimal acquisitionCost;
	private LocalDateTime movementDate;
	private CostingStatus costingStatus;
	private String costingErrorMessage;
	private String movementInOutCorrelationId;
	
	@Override
	public String toString() {
		return "MaterialTransaction [id=" + id + ", correlationId=" + correlationId + ", product=" + product
				+ ", movementType=" + movementType + ", movementQuantity=" + movementQuantity + ", acquisitionCost="
				+ acquisitionCost + ", movementDate=" + movementDate + ", costingStatus=" + costingStatus
				+ ", costingErrorMessage=" + costingErrorMessage + "]";
	}
	
}
