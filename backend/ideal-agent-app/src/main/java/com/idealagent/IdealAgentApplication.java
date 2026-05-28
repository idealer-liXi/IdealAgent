package com.idealagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.idealagent")
public class IdealAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdealAgentApplication.class, args);
    }
}
