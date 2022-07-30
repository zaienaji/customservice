package com.infinite.inventory;

import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.Costing;

@RestController
@RequestMapping("/materialcosting")
public class CostingController {
	
	@Autowired
	private CostingRepository repository;

	@GetMapping()
	public Map<String, TreeSet<Costing>> getCostings(@RequestBody String[] productCorellationIds, @RequestParam(required = false) String where) {
		if (StringUtils.isNotBlank(where))
			return repository.search(where, productCorellationIds);
		
		if (ArrayUtils.isEmpty(productCorellationIds))
			return repository.findAll();
	
		return repository.findByProductCorellationIds(productCorellationIds);
	}
	
	@GetMapping("{id}")
	public Costing getCosting(@PathVariable String id) {
		return repository.find(id);
	}
	
	@GetMapping("/activeonly/{productCorrelationId}")
	public ResponseEntity<Costing> getActiveCostingsByProductCorrelationId(@PathVariable String productCorrelationId) {
		String[] productCorrelationIds = {productCorrelationId};
		Map<String, TreeSet<Costing>> a = repository.findByProductCorellationIds(productCorrelationIds);
		for (TreeSet<Costing> costings : a.values()) {
			return ResponseEntity.ok(costings.last());
		}
		
		return ResponseEntity.noContent().build();
	}

}
