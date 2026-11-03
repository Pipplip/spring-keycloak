package de.pipplip.abfallcontainerservice.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnProperty(name = ["app.swagger.enabled"], havingValue = "true")
class SwaggerUiConfiguration : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/swagger-ui/**")
            .addResourceLocations(
                "classpath:/swagger-ui/",
                "classpath:/META-INF/resources/webjars/swagger-ui/5.18.2/",
            )
    }
}
