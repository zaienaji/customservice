package com.infinite.inventory;

import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.strategy.CostingStrategy;
import com.infinite.inventory.strategy.CostingStrategyFactory;

@RestController
@RequestMapping("/materialtransaction")
public class MaterialTransactionController {
	
	@Autowired
	private MaterialTransactionRepository repository;
	
	@Autowired
	private CostingStrategyFactory costingStrategyFactory;

	@GetMapping()
	public MaterialTransaction[] getMaterialTransactions(@RequestParam(required=false) String[] materialTransactionCorellationIds) {
		if (ArrayUtils.isEmpty(materialTransactionCorellationIds))
			return repository.findAll();

		return repository.findByCorellationIds(materialTransactionCorellationIds);
	}
	
	@PutMapping()
	public ResponseEntity<?>  update(@RequestBody MaterialTransaction[] materialTransactions) {
		
		StringBuilder errorMessages = new StringBuilder();
		
		for (MaterialTransaction record : materialTransactions) {
			
			CostingStrategy costing = costingStrategyFactory.get(record.getProduct());
			Optional<String> errorMessage = costing.updateTransaction(record);
			
			if (errorMessage.isPresent())
				errorMessages.append(errorMessage.get()).append(System.lineSeparator());
		}
		
		if (!errorMessages.isEmpty())
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessages.toString());
		
		return ResponseEntity.ok(materialTransactions);
	}
	
	@GetMapping("/erroronly")
	public MaterialTransaction[] getErrorOnlyMaterialTransaction() {
		return repository.findAllError();
	}
	
	@GetMapping("/search")
	public ResponseEntity<?> search(@RequestParam(required=false) String sqlWhereClause) {
		
		try {
			
			MaterialTransaction[] result = StringUtils.isBlank(sqlWhereClause) ? repository.findAll() :  repository.search(sqlWhereClause);
			
			if (result.length==0)
				return ResponseEntity.noContent().build();
			
			return ResponseEntity.ok(result);
			
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
}
