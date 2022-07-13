package com.infinite.inventory.strategy;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.Product;
import com.infinite.inventory.sharedkernel.ValuationType;

@Component
public class CostingStrategyFactory {
	
	private final HashMap<Product, CostingStrategy> cache = new HashMap<>();
	
	@Autowired
	private ExistingCostingProvider existingCostingProvider;
	
	@Autowired
	private MaterialTransactionRepository materialTransactionRepository;
	
	@Autowired
	private CostingRepository costingRepository;

	public CostingStrategy get(Product product) {
		if (cache.containsKey(product))
			return cache.get(product);
		
		CostingStrategy result = get(product.getValuationType());
		TreeSet<Costing> existingCosting = existingCostingProvider.get(product);
		result.init(existingCosting);
		
		cache.put(product, result);
		
		return result;
		
	}
	
	@PostConstruct
	public void init() {
		Consumer<MaterialTransaction> mTransactionLogger = (x) -> System.out.println(x);
		materialTransactionRepository.addSubscriber(mTransactionLogger);
		
		Consumer<Costing> costingLogger = (x) -> System.out.println(x);
		costingRepository.addSubscriber(costingLogger);
	}
	
	public CostingStrategy get(ValuationType valuationType) {

		switch (valuationType) {

		case MovingAverage:
			return new MovingAverageStrategy(materialTransactionRepository, costingRepository);

		case FIFO:
		case Standard:
		default:
			throw new IllegalArgumentException("not supported valuation type " + valuationType);
		}

	}

}
