server: 
  port: ${server_port!'8082'}
spring:
  application:
    name: ${projectName!'app'}
  profiles:
      active: dev
<#if NACOS?? && NACOS>
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: ${nacos-server!'localhost:8848'}
      config:
        enabled: true
        server-addr: ${nacos-server!'localhost:8848'}
  config:
    import: optional:nacos:
<#elseif ZOOKEEPER?? && ZOOKEEPER>
  cloud:
    zookeeper:
      enabled: true
      connect-string: ${connect-string!'localhost:2181'}
      config:
        enabled: true
        watcher:
          enabled: true
  config:
    import: optional:zookeeper:
</#if>
<#if DUBBO?? && DUBBO>
dubbo: 
  application:
    name: ${r"${spring.application.name}"}
    qos-enable: false
  protocol: 
    name: dubbo
    port: -1
  registry:
<#if NACOS?? && NACOS>
    address: nacos://${r"${spring.cloud.nacos.discovery.server-addr}"}
<#elseif ZOOKEEPER?? && ZOOKEEPER>
    address: zookeeper://${r"${spring.cloud.zookeeper.connect-string}"}
</#if>
</#if>