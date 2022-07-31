package com.infinite.inventory.sharedkernel;

public enum MovementType {
	Unknown,
	CustomerShipment,
	CustomerReturn,
	VendorReceipt,
	VendorReturn,
	PhysicalInventoryIn, 	
	PhysicalInventoryOut,
	MovementIn,
	MovementOut;
}
