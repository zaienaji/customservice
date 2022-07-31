package com.infinite.inventory.sharedkernel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
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
	private String movementOutCorrelationId;
	private String customerShipmentCorrelationId;
	private boolean isError;
}
