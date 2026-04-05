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
        Path mediaPath = Paths.get(mediaLocalRoot);
        String absolutePath = mediaPath.toFile().getAbsolutePath();

        // Map URL pattern /api/v1/media/files/** to the physical storage location
        registry.addResourceHandler("/api/v1/media/files/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
