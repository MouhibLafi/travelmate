package com.travelmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// On exclut UserDetailsServiceAutoConfiguration : on gere nous-memes le login avec JWT,
// donc Spring n'a pas besoin de creer un utilisateur "user" avec un mot de passe genere.
// @ConfigurationPropertiesScan : active les classes de reglages des modules (ex. module 5 : OllamaProperties).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class TravelMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelMateApplication.class, args);
    }
}
