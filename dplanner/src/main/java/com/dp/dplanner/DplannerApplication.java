package com.dp.dplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableJpaAuditing
@SpringBootApplication
public class DplannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DplannerApplication.class, args);
	}

}
