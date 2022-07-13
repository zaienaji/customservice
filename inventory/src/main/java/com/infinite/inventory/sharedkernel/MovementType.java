package com.infinite.inventory.sharedkernel;

public enum MovementType {
	CustomerShipment("Customer Shipment"),
	CustomerReturn("Customer Return"),
	VendorReceipt("Vendor Receipt"),
	VendorReturn("Vendor Return"),
	PhysicalInventoryIn("Physical Inventory In"), 	
	PhysicalInventoryOut("Physical Inventory Out"),
	MovementIn("Movement In"),
	MovementOut("Movement Out"),
	Unknown("Unknown");
	
	private final String label;
	
	private MovementType(String label) {
		this.label = label;
	}
	
	public String toString() {
		return this.label;
	}
}
