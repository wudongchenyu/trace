package pres.emmy.worker.remote.service;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange(value = "lb://consumer")
public interface ConsumerExchange {

    @GetExchange("consumer/index")
    public Mono<String> consume();

}
