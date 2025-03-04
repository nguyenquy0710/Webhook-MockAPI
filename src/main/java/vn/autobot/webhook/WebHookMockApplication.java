package vn.autobot.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class WebHookMockApplication {

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "9");
        System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory", "9");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(WebHookMockApplication.class, args);
    }

}
