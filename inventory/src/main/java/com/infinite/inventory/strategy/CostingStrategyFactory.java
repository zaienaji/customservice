package com.infinite.inventory.strategy;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infinite.inventory.CostingRepository;
import com.infinite.inventory.MaterialTransactionRepository;
import com.infinite.inventory.sharedkernel.Costing;
import com.infinite.inventory.sharedkernel.MaterialTransaction;
import com.infinite.inventory.sharedkernel.Product;

@Component
public class CostingStrategyFactory {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
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
		
		CostingStrategy result = instantiate(product);
		result.init();
		
		cache.put(product, result);
		
		return result;
		
	}
	
	@PostConstruct
	public void init() {
		Consumer<MaterialTransaction> mTransactionLogger = (x) -> logger.info(x.toString());
		materialTransactionRepository.addSubscriber(mTransactionLogger);
		
		Consumer<Costing> costingLogger = (x) -> logger.info(x.toString());
		costingRepository.addSubscriber(costingLogger);
	}
	
	public CostingStrategy instantiate(Product product) {
		switch (product.getValuationType()) {

		case MovingAverage:
			Optional<Costing> costing = existingCostingProvider.get(product);
			return new MovingAverageStrategy(materialTransactionRepository, costingRepository, costing, product);

		case FIFO:
		case Standard:
		default:
			throw new IllegalArgumentException("not supported valuation type " + product.getValuationType());
		}
	}

}
