package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InventoryServiceApplicationTests {
	
	@Autowired
	CostingController costingController;
	
	@Autowired
	MaterialTransactionController materialTransactionController;
	
	@Autowired
	ValuationController valuationController;
	
	@LocalServerPort
	private int port;

	@Autowired
	TestRestTemplate restTemplate;
	
	@Test
	void contextLoads() {
		assertThat(costingController).isNotNull();
		assertThat(materialTransactionController).isNotNull();
		assertThat(valuationController).isNotNull();
	}
	
	@Test
	public void postInventoryValuationsShouldReturnLenghtOfRecords() throws Exception {
		String testdata = "[{\"acquisitionCost\":4800000,\"product\":{\"correlationId\":\"A69E62DBDDD44FF7B3A42100A6462641\",\"valuationType\":\"MovingAverage\"},\"movementType\":\"VendorReceipt\",\"movementQuantity\":600,\"movementDate\":\"2021-06-16T12:06:00\",\"correlationId\":\"62D8C2A0B50D444283660F87E1C63965\",\"costingStatus\":\"NotCalculated\"}]";
		
		HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> request = 
	      new HttpEntity<String>(testdata, headers);
		
		String url = "http://localhost:" + port + "/api/inventory/valuation";
		
		String result = this.restTemplate.postForObject(url, request, String.class);
		
		assertThat(result).isEqualTo("inventory transaction received, lenght: 1");
		
	}

}
