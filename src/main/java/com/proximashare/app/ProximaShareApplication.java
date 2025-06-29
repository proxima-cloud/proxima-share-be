package com.proximashare.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@ComponentScan(basePackages = "com.proximashare")
@EntityScan(basePackages = "com.proximashare.entity")
@EnableJpaRepositories(basePackages = "com.proximashare.repository")
@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Overridezzzzzzzzzz	
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins("http://localhost:5173") // Adjust the allowed origin as needed
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
public class ProximaShareApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProximaShareApplication.class, args);
		System.out.println("ProximaShare Application Started Successfully!");
	}

}
