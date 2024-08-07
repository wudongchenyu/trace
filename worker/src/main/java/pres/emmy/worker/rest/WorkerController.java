package pres.emmy.worker.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pres.emmy.worker.remote.service.ConsumerExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("worker")
public class WorkerController {

    @Autowired
    private ConsumerExchange consumerExchange;

    @GetMapping("index")
    public Mono<String> index() {
        return consumerExchange.consume().map(name -> {
            log.info("调用结果：{}", name);
            return name;
        });
    }

}
