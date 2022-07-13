package com.infinite.inventory.sharedkernel;

import java.util.Objects;

import lombok.Data;

@Data
public class Product {
	
	private String id;
	private String correlationId;
	private ValuationType valuationType;
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Product other = (Product) obj;
		return Objects.equals(correlationId, other.correlationId) && Objects.equals(id, other.id)
				&& valuationType == other.valuationType;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(correlationId, id, valuationType);
	}
	
}
