package com.example.hotel_management_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelManagementProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelManagementProjectApplication.class, args);
	}

}
