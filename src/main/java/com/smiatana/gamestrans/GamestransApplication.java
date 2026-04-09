package com.smiatana.gamestrans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GamestransApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamestransApplication.class, args);
	}

}
