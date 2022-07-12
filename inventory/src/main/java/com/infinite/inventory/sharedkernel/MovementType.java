package com.infinite.inventory.sharedkernel;

public enum MovementType {
	CustomerShipment("Customer Shipment"), 
	VendorReceipt("Vendor Receipt"), 
	PhysicalInventory("Physical Inventory"), 
	MovementIn("MovementIn"),
	MovementOut("MovementOut");
	
	private final String label;
	
	private MovementType(String label) {
		this.label = label;
	}
	
	public String toString() {
		return this.label;
	}
}
