package com.velocity.velocity_management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VelocityManagementApplication {

	private static final Logger log = LoggerFactory.getLogger(VelocityManagementApplication.class.getName());

	public static void main(String[] args) {
		try {
			SpringApplication.run(VelocityManagementApplication.class, args);
		} catch (Throwable e) {
			log.error("FATAL: Spring Boot application failed to start", e);
			System.exit(1);
		}
	}

}
