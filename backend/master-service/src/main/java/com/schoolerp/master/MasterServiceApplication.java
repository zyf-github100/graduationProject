package com.schoolerp.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.schoolerp")
public class MasterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterServiceApplication.class, args);
    }
}
