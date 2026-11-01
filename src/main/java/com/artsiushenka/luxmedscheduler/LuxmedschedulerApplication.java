package com.artsiushenka.luxmedscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class LuxmedschedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LuxmedschedulerApplication.class, args);
	}

}
