package com.looksee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.looksee.browsing.Crawler;

/**
 * Spring wiring for plain-Java classes that live in {@code looksee-browser}
 * and need to be exposed as Spring beans for autowiring by downstream
 * Spring applications.
 *
 * <p>The browser module itself has no Spring dependencies, so any bean
 * exposure happens here in {@code looksee-core}.
 */
@Configuration
public class LookseeCoreBrowserBeansConfiguration {

    /**
     * Exposes a {@link Crawler} instance as a Spring bean. The class itself
     * only contains static methods; the bean exists so that applications
     * that {@code @Autowired} a {@link Crawler} field (e.g. CrawlerAPI) keep
     * working after the {@code @Component} annotation was removed from the
     * class to make {@code looksee-browser} Spring-free.
     *
     * @return a new {@link Crawler} instance
     */
    @Bean
    public Crawler crawler() {
        return new Crawler();
    }
}
