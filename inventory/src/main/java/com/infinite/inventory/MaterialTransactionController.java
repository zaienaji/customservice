package com.infinite.inventory;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

@RestController
@RequestMapping("/materialtransaction")
public class MaterialTransactionController {
	
	@Autowired
	private MaterialTransactionRepository repository;

	@GetMapping()
	public MaterialTransaction[] getMaterialTransactions(@RequestBody String[] materialTransactionCorellationIds) {
		if (ArrayUtils.isEmpty(materialTransactionCorellationIds))
			return repository.findAll();

		return repository.findByCorellationIds(materialTransactionCorellationIds);
	}
	
	@GetMapping("/erroronly")
	public MaterialTransaction[] getErrorOnlyMaterialTransaction() {
		return repository.findAllError();
	}
	
}
