package com.duoc.bffweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.duoc.bffweb")
public class BffWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}