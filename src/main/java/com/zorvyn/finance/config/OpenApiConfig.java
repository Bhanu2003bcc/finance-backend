package com.zorvyn.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Zorvyn Finance Dashboard API")
                        .description("""
                            Finance Data Processing and Access Control Backend.
                            
                            **Roles & Permissions:**
                            - `VIEWER`  — Read-only access to transactions and dashboard
                            - `ANALYST` — Read + write transactions, access analytics
                            - `ADMIN`   — Full access including user management
                            
                            **Authentication:** Use `/api/auth/login` to get a JWT token,
                            then click 'Authorize' and enter: `Bearer <token>`
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Zorvyn FinTech Pvt. Ltd.")
                                .email("support@zorvyn.io")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
