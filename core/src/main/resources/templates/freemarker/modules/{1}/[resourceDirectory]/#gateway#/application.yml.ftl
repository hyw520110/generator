server:
  port: 8080

spring:
  application:
    name: ${projectName}-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: ${projectName}-api
          uri: lb://${projectName}-api
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
