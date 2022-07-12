package com.infinite.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.strategy.CostingStrategy;
import com.infinite.inventory.strategy.CostingStrategyFactory;

@RestController
@RequestMapping("/valuation")
public class ValuationController {
	
	@Autowired
	private CostingStrategyFactory calculatorFactory;
	
	@PostMapping(consumes="application/json")
	public ResponseEntity<String> submitInventoryTransaction(@RequestBody MaterialTransaction[] pendingTransactions) {
		
		for (MaterialTransaction record : pendingTransactions) {
			CostingStrategy calculator = calculatorFactory.get(record.getProduct());
			calculator.appendTransaction(record);
		}
		
		return ResponseEntity.ok("inventory transaction received, lenght: "+pendingTransactions.length);
	}

}
