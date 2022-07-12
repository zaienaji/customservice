package com.infinite.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.strategy.MaterialTransactionRepository;

@RestController
@RequestMapping("/materialtransaction")
public class MaterialTransactionController {
	
	@Autowired
	private MaterialTransactionRepository repository;

	@GetMapping()
	public MaterialTransaction[] getMaterialTransactions(@RequestBody String[] materialTransactionCorellationIds) {

		return repository.findByCorellationIds(materialTransactionCorellationIds);
	}

}
