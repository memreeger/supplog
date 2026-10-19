package com.supplog.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void zeroHopsIgnoresForwardedHeader() {
        MockHttpServletRequest request = request("9.9.9.9, 1.1.1.1");

        assertEquals("10.0.0.1", new ClientIpResolver(0).resolve(request));
    }

    @Test
    void readsFromRightToSkipSpoofedEntries() {
        // İstemci "9.9.9.9" yazdı, Vercel gerçek IP'yi (1.1.1.1), Railway Vercel IP'sini ekledi.
        MockHttpServletRequest request = request("9.9.9.9, 1.1.1.1, 76.76.21.1");

        assertEquals("76.76.21.1", new ClientIpResolver(1).resolve(request));
        assertEquals("1.1.1.1", new ClientIpResolver(2).resolve(request));
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderIsShortOrMissing() {
        assertEquals("10.0.0.1", new ClientIpResolver(2).resolve(request("1.1.1.1")));
        assertEquals("10.0.0.1", new ClientIpResolver(1).resolve(request(null)));
    }

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
