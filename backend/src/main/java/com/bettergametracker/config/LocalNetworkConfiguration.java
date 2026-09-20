package com.bettergametracker.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalNetworkConfiguration implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    private final InetAddress address;
    private final boolean lanEnabled;

    public LocalNetworkConfiguration(@Value("${better-game-tracker.lan-address:}") String lanAddress)
            throws UnknownHostException {
        lanEnabled = !lanAddress.isBlank();
        if (lanEnabled && !lanAddress.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            throw new IllegalArgumentException("LAN address must be a private IPv4 address assigned to this computer");
        }
        address = InetAddress.getByName(lanEnabled ? lanAddress : "127.0.0.1");
        if (lanEnabled && (!(address instanceof Inet4Address) || !address.isSiteLocalAddress())) {
            throw new IllegalArgumentException("LAN address must be within 10.0.0.0/8, 172.16.0.0/12 or 192.168.0.0/16");
        }
    }

    public boolean isLanEnabled() {
        return lanEnabled;
    }

    public String getAddress() {
        return address.getHostAddress();
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        // One explicit interface, never all interfaces through a server.address override.
        factory.setAddress(address);
    }
}
