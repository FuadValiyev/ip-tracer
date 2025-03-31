package com.iptracer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class IpTracerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpTracerApplication.class, args);
    }

}
