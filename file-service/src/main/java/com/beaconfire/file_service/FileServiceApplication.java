package com.beaconfire.file_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FileServiceApplication {

	public static void main(String[] args) {
		System.out.print("FILE SERVICE RUNNING XXXXX");
		SpringApplication.run(FileServiceApplication.class, args);
	}

}
