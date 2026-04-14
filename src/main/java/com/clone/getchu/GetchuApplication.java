package com.clone.getchu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GetchuApplication {

    public static void main(String[] args) {
        SpringApplication.run(GetchuApplication.class, args);
    }
}
