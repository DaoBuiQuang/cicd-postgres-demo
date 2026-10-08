package com.example.cicddemo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "CI/CD PostgreSQL Demo API",
        version = "1.0",
        description = "API demo quản lý người dùng, sản phẩm và đơn hàng."
))
public class OpenApiConfig {
}
