package com.bettergametracker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalNetworkConfigurationTests {
    @Test
    void defaultsToLoopbackEvenIfTheServerAddressWasOverridden() throws Exception {
        var network = new LocalNetworkConfiguration("");
        var factory = new TomcatServletWebServerFactory();
        factory.setAddress(java.net.InetAddress.getByName("0.0.0.0"));
        network.customize(factory);
        assertThat(factory.getAddress().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(network.isLanEnabled()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.1.10"})
    void bindsOnlyTheExplicitPrivateInterface(String address) throws Exception {
        var network = new LocalNetworkConfiguration(address);
        var factory = new TomcatServletWebServerFactory();
        network.customize(factory);
        assertThat(factory.getAddress().getHostAddress()).isEqualTo(address);
        assertThat(network.isLanEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "127.0.0.1", "8.8.8.8", "172.15.0.1", "172.32.0.1",
            "169.254.1.1", "localhost", "example.com", "192.168.1.999", "::", "192.168.1.1/24"})
    void rejectsWildcardPublicOrInvalidLanAddresses(String address) {
        assertThatThrownBy(() -> new LocalNetworkConfiguration(address)).isInstanceOf(Exception.class);
    }
}
