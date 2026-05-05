package com.urbanvogue.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    // RestTemplate lets Order Service call Product Service's API
    // Just like Postman calls an API, but from Java code
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}