package pres.emmy.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class WebClientConfiguration {

    @Bean
    WebClient.Builder microBuilder() {
        return WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().proxyWithSystemProperties()
                                        .responseTimeout(Duration.ofSeconds(10))
                        )
                )
                .codecs(item -> item.defaultCodecs().maxInMemorySize(10*1024*1024));
    }

    @Bean
    HttpServiceProxyFactory factory(WebClient.Builder builder, ReactorLoadBalancerExchangeFilterFunction filterFunction) {
        return HttpServiceProxyFactory.builderFor(
                WebClientAdapter.create(builder.filter(filterFunction)
                        .filter(((request, next) -> {
                            ClientRequest.Builder clientBuilder = ClientRequest.from(request);
                            clientBuilder.header("traceId", MDC.get("traceId"));
                            return next.exchange(clientBuilder.build());
                        }))
                        .build()
                )
        ).build();
    }

}
