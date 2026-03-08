package com.luopc.platform.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class DynamicReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DynamicReportServiceApplication.class, args);
    }
}
