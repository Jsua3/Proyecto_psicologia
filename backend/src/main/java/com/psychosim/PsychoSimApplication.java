package com.psychosim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PsychoSimApplication {
    public static void main(String[] args) {
        SpringApplication.run(PsychoSimApplication.class, args);
    }
}
