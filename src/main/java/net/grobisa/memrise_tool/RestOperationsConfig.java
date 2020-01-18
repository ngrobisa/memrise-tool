package net.grobisa.memrise_tool;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestOperationsConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder().additionalInterceptors(new RestTemplateLoggingInterceptor()).build();
    }
}
