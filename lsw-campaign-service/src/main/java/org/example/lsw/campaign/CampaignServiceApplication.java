package org.example.lsw.campaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application for our PVE Campaign service
 * Launches on port 8083
 */
@SpringBootApplication
public class CampaignServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampaignServiceApplication.class, args);
    }
}
