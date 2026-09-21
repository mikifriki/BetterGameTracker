package com.bettergametracker.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LocalNetworkConfigurationTests {
    @Test
    void defaultsToLoopbackEvenIfTheServerAddressWasOverridden() throws Exception {
        var network = new LocalNetworkConfiguration(false, "");
        var factory = new TomcatServletWebServerFactory();
        factory.setAddress(java.net.InetAddress.getByName("0.0.0.0"));
        network.customize(factory);
        assertThat(factory.getAddress().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(network.isLanEnabled()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.1.10"})
    void bindsOnlyTheExplicitPrivateInterface(String address) throws Exception {
        var network = new LocalNetworkConfiguration(false, address);
        var factory = new TomcatServletWebServerFactory();
        network.customize(factory);
        assertThat(factory.getAddress().getHostAddress()).isEqualTo(address);
        assertThat(network.isLanEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "127.0.0.1", "8.8.8.8", "172.15.0.1", "172.32.0.1",
            "169.254.1.1", "localhost", "example.com", "192.168.1.999", "::", "192.168.1.1/24"})
    void rejectsWildcardPublicOrInvalidLanAddresses(String address) {
        assertThatThrownBy(() -> new LocalNetworkConfiguration(false, address)).isInstanceOf(Exception.class);
    }

    @Test
    void discoversOnlyTheActivePrivateIpv4Address() throws Exception {
        var active = mock(NetworkInterface.class);
        when(active.isUp()).thenReturn(true);
        when(active.inetAddresses()).thenReturn(Stream.of(InetAddress.getByName("192.168.1.33"),
                InetAddress.getByName("203.0.113.1"), InetAddress.getByName("169.254.1.1"),
                InetAddress.getByName("fd00::1")));
        var down = mock(NetworkInterface.class);
        var loopback = mock(NetworkInterface.class);
        when(loopback.isUp()).thenReturn(true);
        when(loopback.isLoopback()).thenReturn(true);
        try (var interfaces = mockStatic(NetworkInterface.class)) {
            interfaces.when(NetworkInterface::networkInterfaces).thenReturn(Stream.of(down, loopback, active));
            var network = new LocalNetworkConfiguration(true, "");
            assertThat(network.getAddress()).isEqualTo("192.168.1.33");
            assertThat(network.isLanEnabled()).isTrue();
        }
    }

    @Test
    void requiresAnOverrideWhenMultiplePrivateAddressesAreAvailable() throws Exception {
        var active = mock(NetworkInterface.class);
        when(active.isUp()).thenReturn(true);
        when(active.inetAddresses()).thenReturn(Stream.of(
                InetAddress.getByName("192.168.1.33"), InetAddress.getByName("10.0.0.2")));
        try (var interfaces = mockStatic(NetworkInterface.class)) {
            interfaces.when(NetworkInterface::networkInterfaces).thenReturn(Stream.of(active));
            assertThatThrownBy(() -> new LocalNetworkConfiguration(true, ""))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("192.168.1.33", "10.0.0.2", "lan-address");
        }
    }

    @Test
    void failsClearlyWhenNoPrivateAddressIsAvailable() {
        try (var interfaces = mockStatic(NetworkInterface.class)) {
            interfaces.when(NetworkInterface::networkInterfaces).thenReturn(Stream.empty());
            assertThatThrownBy(() -> new LocalNetworkConfiguration(true, ""))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("found []", "lan-address");
        }
    }

    @Test
    void explicitAddressAndDefaultLocalStartupDoNotRequireDiscovery() throws Exception {
        try (var interfaces = mockStatic(NetworkInterface.class)) {
            assertThat(new LocalNetworkConfiguration(true, "10.0.0.2").getAddress()).isEqualTo("10.0.0.2");
            assertThat(new LocalNetworkConfiguration(false, "").getAddress()).isEqualTo("127.0.0.1");
            interfaces.verifyNoInteractions();
        }
    }
}
