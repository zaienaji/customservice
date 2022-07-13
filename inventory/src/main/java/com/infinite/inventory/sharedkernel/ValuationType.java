package com.infinite.inventory.sharedkernel;

public enum ValuationType {
	Standard("Standard"), FIFO("FIFO"), MovingAverage("Moving Average"), Unknown("Unknown");
	
	private final String label;
	
	private ValuationType(String label) {
		this.label = label;
	}
	
	public String toString() {
		return this.label;
	}
}
