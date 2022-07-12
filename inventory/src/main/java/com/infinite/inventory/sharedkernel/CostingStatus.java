package com.infinite.inventory.sharedkernel;

public enum CostingStatus {
	NotCalculated("Not Calculated"), Calculated("Calculated"), Error("Error"), Pending("Pending");
	
	private final String label;
	
	private CostingStatus(String label) {
		this.label = label;
	}
	
	public String toString() {
		return this.label;
	}
}
