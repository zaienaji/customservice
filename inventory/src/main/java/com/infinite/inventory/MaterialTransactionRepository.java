package com.infinite.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

@Component
public class MaterialTransactionRepository {
	
	private final List<Consumer<MaterialTransaction>> materialTransctionChangeSubscriber = new LinkedList<Consumer<MaterialTransaction>>();
	
	private final HashMap<String, MaterialTransaction> cacheByCorellationId = new HashMap<>();
	
	public MaterialTransaction findByInOutCorellationId(String corellationId) {
		if (cacheByCorellationId.containsKey(corellationId))
			return cacheByCorellationId.get(corellationId);
		
		return null;
	}

	public void save(MaterialTransaction materialTransaction) {
		if (StringUtils.isBlank(materialTransaction.getId()))
			materialTransaction.setId(UUID.randomUUID().toString());
		
		cacheByCorellationId.put(materialTransaction.getCorrelationId(), materialTransaction);		
		notifyMaterialTransctionChanged(materialTransaction);
	}

	private void notifyMaterialTransctionChanged(MaterialTransaction materialTransaction) {
		for (Consumer<MaterialTransaction> subscriber : materialTransctionChangeSubscriber)
			subscriber.accept(materialTransaction);
	}
	
	public void addSubscriber(Consumer<MaterialTransaction> subscriber) {
		materialTransctionChangeSubscriber.add(subscriber);
	}

	public MaterialTransaction[] findByCorellationIds(String[] materialTransactionCorellationIds) {
		List<MaterialTransaction> result = new ArrayList<>();
		for (String corellationId : materialTransactionCorellationIds) {
			if (cacheByCorellationId.containsKey(corellationId))
				result.add(cacheByCorellationId.get(corellationId));
		}
		
		MaterialTransaction[] resultArr = result.toArray(new MaterialTransaction[result.size()]);
		return resultArr;
	}

}
