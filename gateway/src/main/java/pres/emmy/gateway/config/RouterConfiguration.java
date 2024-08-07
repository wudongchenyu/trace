package pres.emmy.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import pers.wdcy.result.reactor.exception.BusinessException;
import pers.wdcy.result.reactor.exception.GlobalException;
import pers.wdcy.result.reactor.result.Result;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class RouterConfiguration {

    @Bean
    RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder
                .routes()
                .route("consumer", predicateSpec -> predicateSpec.path("/consumer/**")
                        .filters(gatewayFilterSpec -> gatewayFilterSpec.stripPrefix(0)
                                .circuitBreaker(circuitBreaker())
                                .retry(retryConsumer())
//                                .requestRateLimiter()
                        )
                                .uri(URI.create("lb://consumer"))
                )
                .route("worker", predicateSpec -> predicateSpec.path("/worker/**")
                                .filters(gatewayFilterSpec -> gatewayFilterSpec.stripPrefix(0)
                                                .circuitBreaker(circuitBreaker())
                                                .retry(retryConsumer())
//                                .requestRateLimiter()
                                )
                                .uri(URI.create("lb://worker"))
                )
                .build();
    }

    @Bean
    Consumer<SpringCloudCircuitBreakerFilterFactory.Config> circuitBreaker() {
        return config -> config.setName("circuitBreaker")
                .setFallbackUri(URI.create("forward:/fallback"))
                .setStatusCodes(Set.of("500"))
//                .setResumeWithoutError(true)
                .setRouteId("circuitBreaker");
    }

    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        //时间窗口
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                        //时间窗口为60s
                        .slidingWindowSize(60)
                        //在单位时间内最少需要5次才能开启进行统计计算
                        .minimumNumberOfCalls(5)
                        //在单位时间内失败率达到50%后会启动断路器
                        .failureRateThreshold(50)
                        //允许断路器自动由打开状态转换为半开状态
                        .enableAutomaticTransitionFromOpenToHalfOpen()
                        //在半开状态下允许进行正常调用的次数
                        .permittedNumberOfCallsInHalfOpenState(5)
                        //断路器打开状态转化为半开状态需要等待60s
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        //所有异常都当作失败处理
                        .recordExceptions(Throwable.class)
                        .build()
                )
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }

    @Bean
    ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry registry,
                                                                          TimeLimiterRegistry timeLimiterRegistry) {
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory(registry, timeLimiterRegistry, null);
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom().slidingWindowSize(100).build())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build())
                .build());
        return factory;
    }

    @Bean
    Consumer<SpringCloudCircuitBreakerFilterFactory.Config> rateConsumer() {
        return config -> RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1000)
                .build();
    }

    @Bean
    Consumer<RetryGatewayFilterFactory.RetryConfig> retryConsumer() {
        return config -> RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> e instanceof GlobalException)
                .retryExceptions(Throwable.class)
                .retryOnResult(result -> ((Result<?>)result).getCode() == HttpStatus.INTERNAL_SERVER_ERROR.value())
                .ignoreExceptions(BusinessException.class)
                //重试间隔
                .intervalBiFunction(IntervalBiFunction.ofIntervalFunction(attempt ->
                        Double.valueOf(attempt * 500 * 1.5).longValue()))
                .build()
                ;
    }

    @Bean
    RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(
                RateLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(1000)
                        .build()
        );
    }

    @Bean @LoadBalanced
    WebClient.Builder microBuilder() {
        return WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().proxyWithSystemProperties()
                                        .responseTimeout(Duration.ofSeconds(10))
                        )
                )
                .codecs(item -> item.defaultCodecs().maxInMemorySize(10*1024*1024))
                .filter(((request, next) -> {
                    ClientRequest.Builder clientBuilder = ClientRequest.from(request);
                    clientBuilder.header("traceId", MDC.get("traceId"));
                    return next.exchange(clientBuilder.build());
                }));
    }

}
