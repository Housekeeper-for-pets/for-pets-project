package com.forpets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JPA Auditing을 활성화해 BaseEntity의 createdAt, updatedAt이 자동으로 채워지게 합니다.
 */
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class ForPetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForPetsApplication.class, args);
    }

}
