package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SmokeTest {
	
	@Autowired
	ValuationController valuationController;
	
	@Autowired
	CostingController costingController;
	
	@Autowired
	MaterialTransactionController materialTransactionController;
	
	@Test
	void contextLoads() {
		assertThat(costingController).isNotNull();
		assertThat(materialTransactionController).isNotNull();
		assertThat(valuationController).isNotNull();
	}
	
}
