package com.dextre.primerpaso.configuracion;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final String[] origenes;

    public ConfiguracionWeb(@Value("${primerpaso.cors.origenes}") String origenes) {
        this.origenes = Arrays.stream(origenes.split(",")).map(String::strip)
                .filter(origen -> !origen.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/registro/**").allowedOrigins(origenes)
                .allowedMethods("POST", "OPTIONS").allowedHeaders("Content-Type")
                .maxAge(3600);
    }
}
