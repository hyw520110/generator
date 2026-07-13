package ${packagePath}.exception;

<#if (springBootMajor!'3')?string == '4'>
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
<#else>
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
</#if>
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;

import java.nio.charset.StandardCharsets;

@Component
@Order(-1)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String errorJson = String.format("{\"error\": \"%s\"}", ex.getMessage());
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
