package pres.emmy.gateway.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pers.wdcy.result.reactor.result.Result;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Objects;

@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("fallback")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Result<String>> fallback(ServerWebExchange exchange) {
        return Mono.just(exchange)
                .map(change -> {
                    DefaultResponse defaultResponse = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);
                    Exception exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
                    Collection<String> urls = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
                    log.error("服务调用失败URL:{}", urls, exception);
                    if (Objects.nonNull(defaultResponse) && Objects.nonNull(exception)) {
                        return Result.error(String.format("服务调用失败，URL: %s, 原因：%s, 负载：%s:%s，服务：%s", urls, exception.getMessage(),
                                defaultResponse.getServer().getHost(), defaultResponse.getServer().getPort(),
                                defaultResponse.getServer().getServiceId().toLowerCase()));
                    } else {
                        return  Result.error("未知错误");
                    }
                });
    }

}
