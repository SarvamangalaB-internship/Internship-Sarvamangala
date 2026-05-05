package com.urbanvogue.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // This tells Spring Boot to start its magic here
public class ProductServiceApplication {

    public static void main(String[] args) {
        // This line launches the entire application
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}