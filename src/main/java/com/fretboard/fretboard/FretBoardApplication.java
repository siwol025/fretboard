package com.fretboard.fretboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FretBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(FretBoardApplication.class, args);
	}

}
