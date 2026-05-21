package com.guan.rag.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI RAG Lab Platform API",
                description = "V0 项目骨架版 - 系统基础接口",
                version = "V0"
        )
)
public class OpenApiConfig {
}
