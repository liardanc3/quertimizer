package com.quertimizer.global.support;

import com.quertimizer.global.properties.AppSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final AppSecurityProperties appSecurityProperties;

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalizeIp(request.getRemoteAddr());
        if (!appSecurityProperties.getClientIp().isTrustForwardedHeaders() || !isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }

        return resolveForwardedIp(request)
                .map(this::normalizeIp)
                .filter(value -> !value.isBlank())
                .orElse(remoteAddress);
    }

    private Optional<String> resolveForwardedIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return Optional.of(forwardedFor.split(",")[0].trim());
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return Optional.of(realIp.trim());
        }

        return Optional.empty();
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return appSecurityProperties.getClientIp().getTrustedProxies().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(proxy -> matchesProxy(proxy, remoteAddress));
    }

    private boolean matchesProxy(String proxy, String remoteAddress) {
        if (!proxy.contains("/")) {
            return proxy.equals(remoteAddress);
        }

        String[] parts = proxy.split("/", 2);
        try {
            byte[] addressBytes = InetAddress.getByName(remoteAddress).getAddress();
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();
            int prefixLength = Integer.parseInt(parts[1]);
            return addressBytes.length == networkBytes.length && matchesCidr(addressBytes, networkBytes, prefixLength);
        } catch (UnknownHostException | NumberFormatException exception) {
            return false;
        }
    }

    private boolean matchesCidr(byte[] addressBytes, byte[] networkBytes, int prefixLength) {
        if (prefixLength < 0 || prefixLength > addressBytes.length * 8) {
            return false;
        }

        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int index = 0; index < fullBytes; index++) {
            if (addressBytes[index] != networkBytes[index]) {
                return false;
            }
        }

        if (remainingBits == 0) {
            return true;
        }

        int mask = 0xFF << (8 - remainingBits);
        return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return switch (value.trim()) {
            case "0:0:0:0:0:0:0:1" -> "::1";
            default -> value.trim();
        };
    }
}
