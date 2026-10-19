package com.kfokam48.tickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de gestion des tickets de support")
                        .description("""
                                API REST du module Ticket - KFOKAM48 Batch 2 Phase 3.
                                Elle couvre la creation, la consultation, la modification,
                                l'assignation, le cycle de vie et les commentaires des tickets.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Ulrich Siko").email("sikoulrich204@gmail.com")))
                .servers(List.of(new Server().url("/").description("Serveur courant")));
    }
}
