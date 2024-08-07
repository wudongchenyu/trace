package pres.emmy.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Order(1)
@Slf4j
@Component
public class TraceFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        List<String> traces = request.getHeaders().get("traceId");
        if (!ObjectUtils.isEmpty(traces)) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange.mutate().request(
                request.mutate().header("traceId", MDC.get("traceId")).build()
        ).build());
    }
}
