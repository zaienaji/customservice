package com.infinite.inventory;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@PropertySource(value = "application.properties")
public class InventoryServiceApplication {
	
	@Autowired
	Environment env;
	
	@Bean("jdbcTemplate")
	JdbcTemplate jdbcTemplate() {
		DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName(env.getProperty("spring.datasource.driver-class-name"));
		dataSourceBuilder.url(env.getProperty("spring.datasource.url"));
		dataSourceBuilder.username(env.getProperty("spring.datasource.username"));
		dataSourceBuilder.password(env.getProperty("spring.datasource.password"));
		DataSource datasource = dataSourceBuilder.build();
		
		return new JdbcTemplate(datasource);
	}

	public static void main(String[] args) {
		
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

}
