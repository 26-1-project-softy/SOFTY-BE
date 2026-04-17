package com.softy.be.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${swagger.server-url:}")
    private String swaggerServerUrl;

    @Value("${swagger.local-server-url:}")
    private String swaggerLocalServerUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "https://softy-web.vercel.app"
        ));

        addOriginIfPresent(origins, swaggerServerUrl);
        addOriginIfPresent(origins, swaggerLocalServerUrl);

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private void addOriginIfPresent(List<String> origins, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String origin = normalizeToOrigin(value);
        if (StringUtils.hasText(origin) && !origins.contains(origin)) {
            origins.add(origin);
        }
    }

    private String normalizeToOrigin(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                return null;
            }
            int port = uri.getPort();
            if (port < 0) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + port;
        } catch (Exception e) {
            return null;
        }
    }
}
