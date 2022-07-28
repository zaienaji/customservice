package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

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
	@Sql({"/schema.sql"})
	public void postInventoryValuationsShouldReturnLenghtOfRecords() throws Exception {
		String testdata = getTestData();
		
		HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> request = 
	      new HttpEntity<String>(testdata, headers);
		
		String url = "http://localhost:" + port + "/api/inventory/valuation";
		
		String result = this.restTemplate.postForObject(url, request, String.class);
		
		assertThat(result).isEqualTo("inventory transaction received, lenght: 1");
		
	}

	private String getTestData() throws IOException {
		File resource = new ClassPathResource("testdata.json").getFile();
		String testdata = new String(Files.readAllBytes(resource.toPath()));
		return testdata;
	}

}
