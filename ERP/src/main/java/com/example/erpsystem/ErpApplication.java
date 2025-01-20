package com.example.erpsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
//@ComponentScan(basePackages = {
//	"com.example.erpsystem.controller",
//	"com.example.erpsystem.service",
//	"com.example.erpsystem.repository",
//	"com.example.erpsystem.config"
//})
public class ErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpApplication.class, args);
		
	}
	
	

}
