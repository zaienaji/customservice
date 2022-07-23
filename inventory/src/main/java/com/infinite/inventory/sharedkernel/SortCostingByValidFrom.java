package com.infinite.inventory.sharedkernel;

import java.util.Comparator;

public class SortCostingByValidFrom implements Comparator<Costing> {

	@Override
	public int compare(Costing o1, Costing o2) {
		return o1.getValidFrom().compareTo(o2.getValidFrom());
	}

}
