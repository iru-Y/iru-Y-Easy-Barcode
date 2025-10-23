package com.scanner.barcode_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BarcodeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarcodeApiApplication.class, args);
	}

}
