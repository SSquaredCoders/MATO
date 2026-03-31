package com.lshzzz.mato;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Pipeline test - auto build trigger
@SpringBootApplication
@EnableScheduling
public class MatoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatoApplication.class, args);
	}

}
