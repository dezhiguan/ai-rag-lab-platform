package com.guan.rag.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI RAG Lab Platform API",
                description = "V1 文档导入与分块版",
                version = "V1"
        )
)
public class OpenApiConfig {
}
