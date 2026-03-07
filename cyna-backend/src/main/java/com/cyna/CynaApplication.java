package com.cyna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cyna")
public class CynaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynaApplication.class, args);
    }
}
