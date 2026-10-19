package com.supplog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Proxy arkasında gerçek istemci IP'si. X-Forwarded-For'un soldaki girdileri istemci
// tarafından sahte yazılabilir; bu yüzden güvenilen proxy sayısı kadar sağdan okunur.
@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final int trustedProxyHops;

    public ClientIpResolver(
            @Value("${app.rate-limit.trusted-proxy-hops:0}") int trustedProxyHops
    ) {
        this.trustedProxyHops = Math.max(0, trustedProxyHops);
    }

    public String resolve(HttpServletRequest request) {
        if (trustedProxyHops == 0) {
            return request.getRemoteAddr();
        }

        String header = request.getHeader(FORWARDED_FOR);

        if (header == null || header.isBlank()) {
            return request.getRemoteAddr();
        }

        String[] entries = header.split(",");

        if (entries.length < trustedProxyHops) {
            return request.getRemoteAddr();
        }

        String candidate = entries[entries.length - trustedProxyHops].trim();
        return candidate.isEmpty() ? request.getRemoteAddr() : candidate;
    }
}
