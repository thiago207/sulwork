package com.sulwork.backend.comum.config;

import com.sulwork.backend.comum.erro.ErroResposta;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sulworkOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Sulwork API")
                .description("Organizador de eventos de alimentação")
                .version("0.0.1"));
    }

    // toda resposta 4xx/5xx documentada nos controllers usa o schema ErroResposta
    @Bean
    public OpenApiCustomizer respostasDeErro() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ErroResposta.class).forEach(openApi.getComponents()::addSchemas);
            var erro = new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroResposta")));
            openApi.getPaths().values().forEach(caminho -> caminho.readOperations().forEach(operacao ->
                    operacao.getResponses().forEach((codigo, resposta) -> {
                        if (codigo.startsWith("4") || codigo.startsWith("5")) {
                            resposta.setContent(erro);
                        }
                    })));
        };
    }
}
