package com.infinite.inventory.strategy;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

public class MaterialTransactionRepository {
	
	private final List<Consumer<MaterialTransaction>> materialTransctionChangeSubscriber = new LinkedList<Consumer<MaterialTransaction>>();
	
	public MaterialTransaction findByInOutCorellationId(String inoutCorellationId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void save(MaterialTransaction materialTransaction) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		return null;
		
	}

}
