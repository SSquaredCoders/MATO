package com.lshzzz.mato;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatoApplication.class, args);
	}

}
