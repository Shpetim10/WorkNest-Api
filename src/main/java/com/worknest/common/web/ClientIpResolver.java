package com.worknest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized utility that extracts the real client IP from an incoming HTTP request.
 *
 * <h2>Header priority (highest to lowest)</h2>
 * <ol>
 *   <li><b>CF-Connecting-IP</b> – injected by Cloudflare Tunnel / Cloudflare proxy.
 *       Contains the original client IP and is stripped/re-set by Cloudflare, so it
 *       cannot be spoofed by end-clients when Cloudflare is in the path.</li>
 *   <li><b>X-Real-IP</b> – set by nginx and many other single-hop reverse proxies.
 *       Contains only the originating IP (no chain), making it safe to use directly.</li>
 *   <li><b>X-Forwarded-For</b> – standard de-facto header; may contain a comma-separated
 *       chain of addresses. <em>The rightmost IP added by a trusted proxy is the most
 *       reliable</em>, but since we cannot enumerate all trusted proxies here we take the
 *       leftmost value (the original client claim). This is safe only when the outermost
 *       proxy (Cloudflare) has already been handled by the CF-Connecting-IP branch above.
 *       An empty or blank value is skipped.</li>
 *   <li><b>request.getRemoteAddr()</b> – the raw TCP peer address. Used as the final
 *       fallback; behind any reverse proxy this will be the proxy's own address.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   String ip = ClientIpResolver.resolve(httpServletRequest);
 * }</pre>
 *
 * <h2>Production deployment note</h2>
 * When deployed behind a trusted reverse proxy (Cloudflare, nginx, AWS ALB, etc.),
 * ensure {@code server.forward-headers-strategy=NATIVE} (or {@code FRAMEWORK}) is set
 * in {@code application.yml} so that Spring's RemoteAddr/Scheme overrides work correctly
 * in addition to this resolver.
 */
@Slf4j
public final class ClientIpResolver {

    /**
     * Headers inspected in priority order.
     * The first non-blank match wins.
     */
    private static final String[] CANDIDATE_HEADERS = {
            "CF-Connecting-IP",   // Cloudflare Tunnel / Cloudflare proxy - most reliable
            "X-Real-IP",          // nginx and single-hop proxies
            "X-Forwarded-For",    // standard de-facto chain header (leftmost = client)
    };

    private ClientIpResolver() {
        // utility class – no instantiation
    }

    /**
     * Resolves the best available client IP from the request.
     *
     * @param request the incoming HTTP request; must not be {@code null}
     * @return the resolved IP string (never {@code null}, never blank);
     *         falls back to {@link HttpServletRequest#getRemoteAddr()} when no
     *         proxy header is present
     */
    public static String resolve(HttpServletRequest request) {
        for (String header : CANDIDATE_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                // X-Forwarded-For may be a comma-separated chain; take the first entry
                // which is the original client address (other headers are single-value).
                String ip = value.split(",")[0].trim();
                if (!ip.isEmpty()) {
                    log.debug("ClientIpResolver: resolved IP '{}' from header '{}'", ip, header);
                    return ip;
                }
            }
        }

        // Final fallback: raw TCP peer (will be Cloudflare/proxy addr if behind a proxy)
        String remoteAddr = request.getRemoteAddr();
        log.debug("ClientIpResolver: no proxy header found, using RemoteAddr '{}'", remoteAddr);
        return remoteAddr;
    }
}
