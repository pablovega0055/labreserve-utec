package com.utec.dbp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // necesario para que funcione @Async en los listeners de eventos
public class ExamenApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamenApplication.class, args);
    }
}
