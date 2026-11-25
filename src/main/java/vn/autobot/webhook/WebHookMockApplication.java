package vn.autobot.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

// No H2 web console, whatever the configuration says: it would expose the database on the public port
@SpringBootApplication(exclude = H2ConsoleAutoConfiguration.class)
@EnableScheduling
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
