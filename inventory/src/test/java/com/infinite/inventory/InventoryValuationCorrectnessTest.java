package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import com.infinite.inventory.sharedkernel.CostingStatus;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InventoryValuationCorrectnessTest {
	
	@LocalServerPort
	private int port;

	@Autowired
	TestRestTemplate restTemplate;
	
	@ParameterizedTest
	@CsvSource({
		"D8B2916974AE4F3D80C79FAC27E2EB2D,Calculated,10000",
		"1FE6DDD27E014FAC8DD53F5C01892851,Calculated,8000",
		"174C8CE7718643A9AA487E16CD29B55A,Error,",
		"D8DDDF7EBF274F749E7C5CE5FE393D8F,,"})
	@Sql({"/schema.sql"})
	public void testInventoryValuationCorrectness(String correlationId, CostingStatus costingStatus, Double acquisitionCost) throws Exception {
		
		testPostMaterialTransaction();
		testCorrectnessAcquisitionCostCalculation(correlationId, costingStatus, acquisitionCost==null ? null : new BigDecimal(acquisitionCost));
	}

	private void testCorrectnessAcquisitionCostCalculation(String correlationId, CostingStatus costingStatus, BigDecimal acquisitionCost) throws IOException, JSONException {
		String url = "http://localhost:" + port + "/api/inventory/materialtransaction?materialTransactionCorellationIds="+correlationId;
		
		HashMap<String, String> param = new HashMap<>();
		param.put("materialTransactionCorellationIds", correlationId);

		String body = this.restTemplate.getForObject(url, String.class);
		JSONArray responseBody = new JSONArray(body);
		if (costingStatus==null) {
		
			assertThat(responseBody.length()==0);
			return;
		}
		
		assertThat(responseBody.length() >= 1);
		
		JSONObject mTransaction = responseBody.getJSONObject(0);
		CostingStatus actualCostingStatus = CostingStatus.valueOf((String)mTransaction.get("costingStatus"));
		assertThat(actualCostingStatus == costingStatus);
		
		if (costingStatus==CostingStatus.Calculated) {
			BigDecimal actualAcquisitionCost = new BigDecimal((Double)mTransaction.get("acquisitionCost"));
			assertThat(acquisitionCost).isEqualTo(actualAcquisitionCost);
		}
	}

	private void testPostMaterialTransaction() throws IOException {
		String testdata = getMaterialCostingTestData();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(testdata, headers);

		String url = "http://localhost:" + port + "/api/inventory/valuation";

		String result = this.restTemplate.postForObject(url, request, String.class);

		assertThat(result).isEqualTo("inventory transaction received, lenght: 26");

	}

	private String getMaterialCostingTestData() throws IOException {
		File resource = new ClassPathResource("testdata.json").getFile();
		String testdata = new String(Files.readAllBytes(resource.toPath()));
		return testdata;
	}

}
