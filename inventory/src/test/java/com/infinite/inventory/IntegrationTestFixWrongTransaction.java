package com.infinite.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import com.infinite.inventory.util.Util;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IntegrationTestFixWrongTransaction {

	@LocalServerPort
	private int port;

	@Autowired
	TestRestTemplate restTemplate;
	
	@Test
	public void fixWrongTransaction_correctness_positive() throws JSONException {
			
		String testdata = "[\r\n"
				+ "    {\r\n"
				+ "        \"correlationId\": \"E943527137A24A9E8F9209C058DF2A1D\",\r\n"
				+ "        \"product\": {\r\n"
				+ "            \"correlationId\": \"D331AACC8E5F425A9129F530002EA669\",\r\n"
				+ "            \"valuationType\": \"MovingAverage\"\r\n"
				+ "        },\r\n"
				+ "        \"movementType\": \"VendorReceipt\",\r\n"
				+ "        \"movementQuantity\": 50,\r\n"
				+ "        \"acquisitionCost\": 50000,\r\n"
				+ "        \"movementDate\": \"2021-08-23T11:08:00\",\r\n"
				+ "        \"costingStatus\": \"NotCalculated\"\r\n"
				+ "    }\r\n"
				+ "]";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(testdata, headers);

		String url = "http://localhost:" + port + "/api/inventory/valuation/addtop";

		restTemplate.postForObject(url, request, String.class);
		
		url = "http://localhost:" + this.port + "/api/inventory/materialtransaction/erroronly";
		String body = this.restTemplate.getForObject(url, String.class);
		
		assertThat(body).isEqualTo("[]");
	}

	@BeforeAll
	public static void testPostMaterialTransaction(@Autowired TestRestTemplate restTemplate, @LocalServerPort int port, @Autowired JdbcTemplate jdbcTemplate) throws IOException {
		
		deleteExistingRecords(jdbcTemplate);
		
		String testdata = getMaterialCostingTestData();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(testdata, headers);

		String url = "http://localhost:" + port + "/api/inventory/valuation";

		restTemplate.postForObject(url, request, String.class);
		
		Util.SleepInMilis(30);
	}

	private static void deleteExistingRecords(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.execute("delete from materialtransaction");
		jdbcTemplate.execute("delete from costing");
	}

	private static String getMaterialCostingTestData() throws IOException {
		File resource = new ClassPathResource("testdata.json").getFile();
		String testdata = new String(Files.readAllBytes(resource.toPath()));
		return testdata;
	}

}
