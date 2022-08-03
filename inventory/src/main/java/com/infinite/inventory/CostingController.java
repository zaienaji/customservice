package com.infinite.inventory;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.Costing;

@RestController
@RequestMapping("/materialcosting")
public class CostingController {
	
	@Autowired
	private CostingRepository repository;
	
	@GetMapping("/activeonly/{productCorrelationId}")
	public ResponseEntity<Costing> getActiveCostingsByProductCorrelationId(@PathVariable String productCorrelationId) {
		String[] productCorrelationIds = {productCorrelationId};
		Map<String, Costing> costingsByProduct = repository.findByProductCorellationIds(productCorrelationIds);
		for (Costing costings : costingsByProduct.values()) {
			return ResponseEntity.ok(costings);
		}
		
		return ResponseEntity.noContent().build();
	}

}
