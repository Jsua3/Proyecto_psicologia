package com.psychosim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI siepOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIEP API")
                        .description("Sistema de Entrenamiento Psicosocial — API REST académica")
                        .version("1.0"));
    }
}
