spring:
  threads:
    virtual:
      enabled: true

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml,classpath*:mappers/**/*.xml

dubbo:
  consumer:
    check: false
    timeout: 1500
    retries: 0

internal-header:
  enabled: ${internal_header_enabled!'false'}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
