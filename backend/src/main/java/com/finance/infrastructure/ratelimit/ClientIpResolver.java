package com.finance.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Works out the caller's address. Behind the host's proxy the TCP peer is the proxy, so the real address arrives in
 * X-Forwarded-For. Anyone can send that header themselves, and each proxy appends the address it saw to the RIGHT, so the
 * leftmost entries are whatever the caller claimed. Only the entry added by our own trusted proxies can be believed:
 * with N trusted proxy hops, that is the Nth from the right. N = 0 ignores the header entirely (direct connections).
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private final int trustedProxyHops;
    private final AtomicBoolean reportedShape = new AtomicBoolean();

    public ClientIpResolver(@Value("${ratelimit.trusted-proxy-hops:1}") int trustedProxyHops) {
        this.trustedProxyHops = trustedProxyHops;
    }

    public String resolve(HttpServletRequest request) {
        if (trustedProxyHops > 0) {
            String header = request.getHeader("X-Forwarded-For");
            if (header != null && !header.isBlank()) {
                String[] entries = header.split(",");
                if (reportedShape.compareAndSet(false, true)) {
                    // Once, counts only (no addresses): lets an operator confirm the hop setting matches the host's proxy chain.
                    log.info("Rate limiting: first X-Forwarded-For had {} entries, trusting hop {} from the right", entries.length, trustedProxyHops);
                }
                int index = entries.length - trustedProxyHops;
                if (index >= 0) {
                    String candidate = entries[index].strip();
                    if (!candidate.isEmpty() && candidate.length() <= 45) {
                        return candidate;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
