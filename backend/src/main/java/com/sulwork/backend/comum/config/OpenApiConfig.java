package com.sulwork.backend.comum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sulworkOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Sulwork API")
                .description("Organizador de eventos de alimentação: eventos, participantes, "
                        + "categorias, itens, contribuições, cobertura e restrições alimentares.")
                .version("0.0.1"));
    }
}
