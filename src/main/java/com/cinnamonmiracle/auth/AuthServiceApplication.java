package com.cinnamonmiracle.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * scanBasePackages includes com.cinnamonmiracle so the shared beans in the
 * {@code common} module (JwtUtil, JwtAuthInterceptor, GlobalExceptionHandler)
 * are picked up.
 */
@SpringBootApplication(scanBasePackages = "com.cinnamonmiracle")
@EnableJpaAuditing
public class AuthServiceApplication {

    public static void main(String[] args) {
        System.setProperty("net.bytebuddy.experimental", "true");
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
