package com.proximashare.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.proximashare")
@EntityScan(basePackages = "com.proximashare.entity")
@EnableJpaRepositories(basePackages = "com.proximashare.repository")

public class ProximaShareApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProximaShareApplication.class, args);
		System.out.println("ProximaShare Application Started Successfully!");
	}

}
