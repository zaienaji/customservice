package com.infinite.inventory;

import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.Product;

@RestController
@RequestMapping("/materialcosting")
public class CostingController {
	
	@Autowired
	private CostingRepository repository;

	@GetMapping()
	public Map<Product, TreeSet<Costing>> getCostings(@RequestBody String[] productCorellationIds) {
	
		return repository.findByProductCorellationIds(productCorellationIds);
	}

}
