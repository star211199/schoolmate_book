package com.schoolmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 大学同学录 - 后端启动类。
 *
 * @author Albot
 */
@EnableScheduling
@SpringBootApplication
public class SchoolmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolmateApplication.class, args);
    }
}
