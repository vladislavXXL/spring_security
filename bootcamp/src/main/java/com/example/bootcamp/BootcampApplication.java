package com.example.bootcamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@SpringBootApplication
public class BootcampApplication {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Component("uuid")
    class UuidService {
        public String buildUuid() {
            return UUID.randomUUID().toString();
        }
    }

//    @Bean
//    Bar bar(Foo foo, @Value("#{uuid.buildUuid()}") String uuid) {
//        return new Bar(foo, uuid);
//    }

    public static void main(String[] args) {
        SpringApplication.run(BootcampApplication.class, args);
    }

    @Component
    @Aspect
    class LoggingAspect {
        private final Log log = LogFactory.getLog(getClass());

        @Around("execution(* com.example..*.*(..))")
        public Object log(ProceedingJoinPoint pjp) throws Throwable {
            this.log.info("Before: " + pjp.toString());
            Object object = pjp.proceed();
            this.log.info("After: " + pjp.toString());
            return object;
        }
    }

    @Component
    class LoggingFilter implements javax.servlet.Filter {
        private final Log log = LogFactory.getLog(getClass());

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            Assert.isTrue(request instanceof HttpServletRequest, "This assumes you have an HTTP request");
            HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);
            String uri = httpServletRequest.getRequestURI();
            this.log.info("New request for " + uri + ".");
            long time = System.currentTimeMillis();
            chain.doFilter(request, response);
            long delta = System.currentTimeMillis() - time;
            this.log.info("Request for " + uri + " took " + delta + " ms");
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void destroy() {

        }
    }

    @RestController
    class IsbnRestController {
        private final RestTemplate restTemplate;

        public IsbnRestController(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        @GetMapping("/books/{isbn}")
        String lookupBookByIsbn(@PathVariable("isbn") String isbn) {
            ResponseEntity<String> exchange = this.restTemplate
                    .exchange("https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn, HttpMethod.GET,
                            null, String.class);
            return exchange.getBody();
        }
    }

    @Component
    class Bar {
        private final Foo foo;
        private final Log log = LogFactory.getLog(getClass());

        public Bar(Foo foo, @Value("#{uuid.buildUuid()}") String uuid,
                   @Value("#{2 > 1}") boolean proceed) {
            this.foo = foo;
            this.log.info("UUID: " + uuid);
            this.log.info("Proceed: " + proceed);
        }
    }

    @Component
    class Foo {

    }
}
