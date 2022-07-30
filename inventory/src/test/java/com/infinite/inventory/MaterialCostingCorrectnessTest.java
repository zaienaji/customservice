package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MaterialCostingCorrectnessTest {

	@LocalServerPort
	private int port;

	@Autowired
	TestRestTemplate restTemplate;

	@ParameterizedTest
	@CsvSource({"32D2A76A81584741AF1CFD73F3BAD509,10000,1999"})
	@Sql({ "/schema.sql" })
	public void testMaterialCostingCorrectness(String productCorrelationId, Double unitCost, Double totalQuantity) throws Exception {

		String url = "http://localhost:" + this.port + "/api/inventory/materialcosting/activeonly/" + productCorrelationId;

		String body = this.restTemplate.getForObject(url, String.class);
		JSONObject mCosting = new JSONObject(body);
		
		BigDecimal actualUnitCost = new BigDecimal(mCosting.get("unitCost").toString());
		assertThat(actualUnitCost.compareTo(new BigDecimal(unitCost))==0);
		
		BigDecimal actualTotalQuantity = new BigDecimal(mCosting.get("totalQty").toString());
		assertThat(actualTotalQuantity.compareTo(new BigDecimal(totalQuantity))==0);
	}

	@BeforeAll
	public static void testPostMaterialTransaction(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port) throws IOException {
		String testdata = getMaterialCostingTestData();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(testdata, headers);

		String url = "http://localhost:" + port + "/api/inventory/valuation";

		String result = restTemplate.postForObject(url, request, String.class);

		assertThat(result).isEqualTo("inventory transaction received, lenght: 26");

	}

	private static String getMaterialCostingTestData() throws IOException {
		File resource = new ClassPathResource("testdata.json").getFile();
		String testdata = new String(Files.readAllBytes(resource.toPath()));
		return testdata;
	}

}
