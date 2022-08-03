package com.infinite.inventory.strategy;

import java.util.HashMap;
import java.util.LinkedList;
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
import com.infinite.inventory.sharedkernel.ValuationType;

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
		
		CostingStrategy result = get(product.getValuationType());
		LinkedList<Costing> existingCosting = existingCostingProvider.get(product);
		result.init(existingCosting);
		
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
	
	public CostingStrategy get(ValuationType valuationType) {
		switch (valuationType) {

		case MovingAverage:
			return new MovingAverageStrategy(materialTransactionRepository, costingRepository); //TODO refactor -> use bean instead

		case FIFO:
		case Standard:
		default:
			throw new IllegalArgumentException("not supported valuation type " + valuationType);
		}
	}

}
