package com.bettergametracker;

import com.bettergametracker.config.PersistenceRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportRuntimeHints(PersistenceRuntimeHints.class)
public class BetterGameTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetterGameTrackerApplication.class, args);
    }
}
