package com.softy.be.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String API_TITLE = "Softy API";
    private static final String API_DESCRIPTION = "Softy API 문서";
    private static final String API_VERSION = "1.0.0";

    @Value("${swagger.server-url:}")
    private String serverUrl;

    @Value("${swagger.local-server-url:}")
    private String localServerUrl;

    @Bean
    public OpenAPI openAPI() {
        List<Server> servers = new ArrayList<>();
        if (serverUrl != null && !serverUrl.isBlank()) {
            servers.add(new Server().url(serverUrl).description("운영 서버"));
        }
        if (localServerUrl != null && !localServerUrl.isBlank()) {
            servers.add(new Server().url(localServerUrl).description("로컬 서버"));
        }

        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .description(API_DESCRIPTION)
                        .version(API_VERSION))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                        ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
