package com.worknest.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.media.local-root:storage/media}")
    private String mediaLocalRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path mediaPath = Paths.get(mediaLocalRoot).toAbsolutePath().normalize();
        String absolutePath = mediaPath.toFile().getAbsolutePath();

        // Primary media URL
        registry.addResourceHandler("/api/v1/media/files/**")
                .addResourceLocations("file:" + absolutePath + "/");

        // Backward-compatible URL used by some clients
        registry.addResourceHandler("/storage/media/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
