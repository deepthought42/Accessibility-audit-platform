package com.crawlerApi.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

@Target({ElementType.TYPE, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping
public @interface ApiVersion {
    String value() default "v1";
    String[] produces() default MediaType.APPLICATION_JSON_VALUE;
    String[] consumes() default MediaType.APPLICATION_JSON_VALUE;
}