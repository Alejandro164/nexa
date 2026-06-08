// package com.empresa.proyecto.config;

// import org.springframework.context.annotation.Configuration;
// import
// org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration
// public class WebMvcConfig implements WebMvcConfigurer {

// @Override
// public void addResourceHandlers(ResourceHandlerRegistry registry) {
// // Exponemos SOLO archivos .js dentro de templates/ por seguridad.
// // El patrón/**/*.js garantiza que nadie pueda descargar tus plantillas HTML
// de Thymeleaf (riesgo de seguridad).
// registry.addResourceHandler("/js-components/**/*.js")
// .addResourceLocations("classpath:/templates/");
// }
// }
