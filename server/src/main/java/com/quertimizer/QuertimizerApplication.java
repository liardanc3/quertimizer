package com.quertimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuertimizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuertimizerApplication.class, args);
	}

}
