package com.bettergametracker.config;

import java.io.IOException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalBrowserLauncher {
    private final boolean enabled;
    private final LocalNetworkConfiguration network;

    public LocalBrowserLauncher(@Value("${better-game-tracker.open-browser:false}") boolean enabled,
            LocalNetworkConfiguration network) {
        this.enabled = enabled;
        this.network = network;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof ServletWebServerApplicationContext context)) {
            return;
        }
        String scheme = context.getEnvironment().getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http";
        String url = scheme + "://" + network.getAddress() + ":" + context.getWebServer().getPort() + "/";
        LoggerFactory.getLogger(LocalBrowserLauncher.class).info("Open {} in your browser{}", url,
                network.isLanEnabled() ? " (trusted LAN: all reachable devices have full library access)" : "");
        if (!enabled) {
            return;
        }
        String os = System.getProperty("os.name");
        try {
            if (os.startsWith("Windows")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.startsWith("Mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException exception) {
            LoggerFactory.getLogger(LocalBrowserLauncher.class).warn("Open {} in your browser", url, exception);
        }
    }
}
