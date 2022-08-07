package com.infinite.inventory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

@Component
public class ThreadPool {
	
	private final ExecutorService pool;
	
	public ThreadPool() {
		
		int cores = Runtime.getRuntime().availableProcessors();
		if (cores>=4)
			cores = cores/2;
			
		pool = Executors.newFixedThreadPool(cores);
	}
	
	public void execute(Runnable command) {
		pool.execute(command);
	}

}
