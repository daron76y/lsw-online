package org.example.lsw.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: the single entry point for all client requests
 * Routes (these are all defined in the application.yml file):
 *   /api/auth/__     --> user-service:8081
 *   /api/users/__    --> user-service:8081
 *   /api/parties/__  --> party-service:8082
 *   /api/campaign/__ --> campaign-service:8083
 *   /api/battle/__   --> battle-service:8084
 *   /api/pvp/__      --> pvp-service:8085
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
