package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import com.infinite.inventory.sharedkernel.CostingStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	TestRestTemplate restTemplate;
	
	@Test
	public void testMaterialTransactionRespository_shouldAbleToQueryErrorRecord() throws JSONException {
		String url = "http://localhost:" + this.port + "/api/inventory/materialtransaction/erroronly";
		String body = this.restTemplate.getForObject(url, String.class);
		
		JSONArray responseBody = new JSONArray(body);
		assertThat(responseBody.length()).isEqualTo(1);
		
		JSONObject mTransaction = responseBody.getJSONObject(0);
		String actualCorrelationId = (String) mTransaction.get("correlationId");
		assertThat(actualCorrelationId).isEqualTo("174C8CE7718643A9AA487E16CD29B55A");		
	}
	

	@Test
	public void testMaterialTransactionRespository_shouldContainAllRecords() throws JSONException {
		String url = "http://localhost:" + this.port + "/api/inventory/materialtransaction";
		String body = this.restTemplate.getForObject(url, String.class);
		
		JSONArray responseBody = new JSONArray(body);
		assertThat(responseBody.length()).isEqualTo(26);
	}
	
	@ParameterizedTest
	@CsvSource({ 
		"D8B2916974AE4F3D80C79FAC27E2EB2D,Calculated,10000", 
		"1FE6DDD27E014FAC8DD53F5C01892851,Calculated,8000",
	    "174C8CE7718643A9AA487E16CD29B55A,Error,", 
	    "D8DDDF7EBF274F749E7C5CE5FE393D8F,NotCalculated," })
	public void testInventoryValuationCorrectness(String correlationId, CostingStatus costingStatus,
	    Double acquisitionCost) throws Exception {

		String url = "http://localhost:" + this.port
		    + "/api/inventory/materialtransaction?materialTransactionCorellationIds=" + correlationId;

		String body = this.restTemplate.getForObject(url, String.class);
		JSONArray responseBody = new JSONArray(body);
		
		assertThat(responseBody.length()).isEqualTo(1);

		JSONObject mTransaction = responseBody.getJSONObject(0);
		CostingStatus actualCostingStatus = CostingStatus.valueOf((String) mTransaction.get("costingStatus"));
		assertThat(actualCostingStatus == costingStatus);

		if (costingStatus == CostingStatus.Calculated) {
			BigDecimal actualAcquisitionCost = new BigDecimal((Double) mTransaction.get("acquisitionCost"));
			assertThat(actualAcquisitionCost).isEqualTo(new BigDecimal(acquisitionCost));
		}
	}
	
	@ParameterizedTest
	@CsvSource({"D331AACC8E5F425A9129F530002EA669"})
	public void testMaterialCostingCorrectness_negative(String productCorrelationId) throws Exception {

		String url = "http://localhost:" + this.port + "/api/inventory/materialcosting/activeonly/" + productCorrelationId;
		
		HttpHeaders headers=new HttpHeaders();
		headers.add("Content-Type",MediaType.APPLICATION_JSON.toString());
		            
		HttpEntity<String> httpEntity = new HttpEntity<String>(null, headers);
		
		ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@ParameterizedTest
	@CsvSource({
		"32D2A76A81584741AF1CFD73F3BAD509,10000,1999",
		"A69E62DBDDD44FF7B3A42100A6462641,8000,594"})
	public void testMaterialCostingCorrectness_positive(String productCorrelationId, Double unitCost, Double totalQuantity) throws Exception {

		String url = "http://localhost:" + this.port + "/api/inventory/materialcosting/activeonly/" + productCorrelationId;

		String body = this.restTemplate.getForObject(url, String.class);
		JSONObject mCosting = new JSONObject(body);
		
		BigDecimal actualUnitCost = new BigDecimal(mCosting.get("unitCost").toString());
		assertThat(actualUnitCost.compareTo(new BigDecimal(unitCost))).isEqualTo(0);
		
		BigDecimal actualTotalQuantity = new BigDecimal(mCosting.get("totalQty").toString());
		assertThat(actualTotalQuantity.compareTo(new BigDecimal(totalQuantity))).isEqualTo(0);
	}

	@BeforeAll
	public static void testPostMaterialTransaction(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port, @Autowired JdbcTemplate jdbcTemplate) throws IOException {
		
		deleteExistingMaterialCosting(jdbcTemplate);
		
		String testdata = getMaterialCostingTestData();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(testdata, headers);

		String url = "http://localhost:" + port + "/api/inventory/valuation";

		restTemplate.postForObject(url, request, String.class);		
	}

	private static void deleteExistingMaterialCosting(JdbcTemplate jdbcTemplate) {
		String query = "delete from materialtransaction";
		jdbcTemplate.execute(query);
	}

	private static String getMaterialCostingTestData() throws IOException {
		File resource = new ClassPathResource("testdata.json").getFile();
		String testdata = new String(Files.readAllBytes(resource.toPath()));
		return testdata;
	}

}
