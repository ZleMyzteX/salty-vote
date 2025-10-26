package er.codes.saltyvote.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    // Is only used to allow the swagger-ui to work with JWT Bearer tokens
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .addSecurityItem(SecurityRequirement().addList(("Bearer Authentication")))
            .components(
                Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .bearerFormat("JWT")
                            .scheme("bearer"),
                    ),
            ).info(
                Info()
                    .title("SaltyVote API")
                    .description("API documentation for the saltyvote application.")
                    .version("1.0"),
            )
}
