package com.finance.ratelimit;

import com.finance.infrastructure.ratelimit.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void believesOnlyTheEntryAddedByTheTrustedProxy() {
        ClientIpResolver oneHop = new ClientIpResolver(1);
        assertThat(oneHop.resolve(request("203.0.113.7"))).isEqualTo("203.0.113.7");
        // The caller put the first two entries there itself; the proxy appended the real one last.
        assertThat(oneHop.resolve(request("1.1.1.1, 2.2.2.2, 203.0.113.7"))).isEqualTo("203.0.113.7");
    }

    @Test
    void twoTrustedProxiesMeansTheSecondFromTheRight() {
        ClientIpResolver twoHops = new ClientIpResolver(2);
        assertThat(twoHops.resolve(request("6.6.6.6, 203.0.113.7, 172.16.0.1"))).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToThePeerWhenTheHeaderIsMissingTooShortOrIgnored() {
        assertThat(new ClientIpResolver(1).resolve(request(null))).isEqualTo("10.0.0.9");
        assertThat(new ClientIpResolver(2).resolve(request("203.0.113.7"))).isEqualTo("10.0.0.9");
        assertThat(new ClientIpResolver(0).resolve(request("203.0.113.7"))).isEqualTo("10.0.0.9");
        assertThat(new ClientIpResolver(1).resolve(request("  "))).isEqualTo("10.0.0.9");
    }
}
