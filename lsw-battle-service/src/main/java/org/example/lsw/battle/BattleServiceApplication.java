package org.example.lsw.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point for the Spring Boot application running on port 8084 for the battle service.
 */
@SpringBootApplication
public class BattleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BattleServiceApplication.class, args);
    }
}
