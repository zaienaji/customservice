package com.infinite.inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
		
		Optional<String> errorMessage = assignId(materialTransactions);
		if (errorMessage.isPresent())
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage.toString());
		
		StringBuilder errorMessages = new StringBuilder();
		for (MaterialTransaction record : materialTransactions) {
			
			CostingStrategy costing = costingStrategyFactory.get(record.getProduct());
			Optional<String> updateStatus = costing.updateTransaction(record);
			
			if (updateStatus.isPresent())
				errorMessages.append(updateStatus.get()).append(System.lineSeparator());
		}
		
		if (!errorMessages.isEmpty())
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessages.toString());
		
		return ResponseEntity.ok(materialTransactions);
	}

	private Optional<String> assignId(MaterialTransaction[] materialTransactions) {
		
		Map<String, MaterialTransaction> materialTransactionsByCorrelationId = mapMaterialTransactionByCorrelationId(materialTransactions);
		
		StringBuilder errorMessages = new StringBuilder();
		for (MaterialTransaction materialTransaction : materialTransactions) {
			
			String correlationId = materialTransaction.getCorrelationId();
			
			if (materialTransactionsByCorrelationId.containsKey(correlationId)) {
				MaterialTransaction materialTransactionFromDb = materialTransactionsByCorrelationId.get(materialTransaction.getCorrelationId());
				materialTransaction.setId(materialTransactionFromDb.getId());
			} else {
				errorMessages.append("cannot find material transaction from db with correlation id "+correlationId).append(System.lineSeparator());
			}
		}
		
		if (errorMessages.isEmpty())
			return Optional.empty();
		
		return Optional.of(errorMessages.toString());
	}

	private Map<String, MaterialTransaction> mapMaterialTransactionByCorrelationId(MaterialTransaction[] materialTransactions) {
		
		List<String> correlationIds = Arrays.stream(materialTransactions).map(MaterialTransaction::getCorrelationId).collect(Collectors.toList());
		MaterialTransaction[] materialTransactionFromDb = repository.findByCorellationIds(correlationIds.toArray(new String[0]));
		
		return Arrays.stream(materialTransactionFromDb).collect(Collectors.toMap(MaterialTransaction::getCorrelationId, Function.identity()));
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
