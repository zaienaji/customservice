package com.infinite.inventory;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

@RestController
@RequestMapping("/materialtransaction")
public class MaterialTransactionController {
	
	@Autowired
	private MaterialTransactionRepository repository;

	@GetMapping()
	public MaterialTransaction[] getMaterialTransactions(@RequestParam(required=false) String[] materialTransactionCorellationIds) {
		if (ArrayUtils.isEmpty(materialTransactionCorellationIds))
			return repository.findAll();

		return repository.findByCorellationIds(materialTransactionCorellationIds);
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
