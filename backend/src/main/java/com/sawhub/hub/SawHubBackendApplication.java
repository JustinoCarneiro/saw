package com.sawhub.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SawHubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SawHubBackendApplication.class, args);
	}

}
