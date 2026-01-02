package com.nexus.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NexusChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusChatApplication.class, args);
    }

}
