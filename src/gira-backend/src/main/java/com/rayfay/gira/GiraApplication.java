package com.rayfay.gira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GiraApplication {
    public static void main(String[] args) {
        SpringApplication.run(GiraApplication.class, args);
    }
}