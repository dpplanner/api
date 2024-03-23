package com.dp.dplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

//
@EnableScheduling
@EnableCaching
@EnableJpaAuditing
@SpringBootApplication
public class DplannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DplannerApplication.class, args);
	}

}
