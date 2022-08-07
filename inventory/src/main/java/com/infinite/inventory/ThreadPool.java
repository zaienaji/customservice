package com.infinite.inventory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

@Component
public class ThreadPool {
	
	private final ExecutorService pool = Executors.newFixedThreadPool(1_000_000);
	
	public void execute(Runnable command) {
		pool.execute(command);
	}

}
