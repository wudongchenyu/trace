package pres.emmy.consumer.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("consumer")
public class ConsumerController {

    @GetExchange("index")
    public Mono<String> consume() {
        return Mono.just("Hello World")
                .doOnNext(i -> log.info("Consumed: {}", i));
    }

}
