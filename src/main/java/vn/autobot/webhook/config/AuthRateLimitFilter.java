package vn.autobot.webhook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.autobot.webhook.service.McpRateLimiter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Slows down password guessing and mass registration. Limits are per client address, so nobody can lock another
 * person's account. In the security chain after the CSRF check; not a bean, or it would also run outside it.
 */
@RequiredArgsConstructor
class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int LOGINS_PER_ADDRESS = 30;
    private static final int LOGINS_PER_ADDRESS_AND_USER = 10;
    private static final int REGISTRATIONS_PER_ADDRESS = 5;

    private final McpRateLimiter limiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int retryAfter = "POST".equals(request.getMethod()) ? retryAfter(request) : 0;
        if (retryAfter > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        chain.doFilter(request, response);
    }

    /** @return 0 when the request is allowed, otherwise the seconds to wait */
    private int retryAfter(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        switch (request.getServletPath()) {
            case "/login":
                int wait = limiter.tryAcquire("login:" + address, LOGINS_PER_ADDRESS);
                String username = request.getParameter("username");
                // Second, so one address cannot fill the limiter with made-up usernames
                return wait > 0 || username == null ? wait
                        : limiter.tryAcquire("login:" + address + ":" + username.trim().toLowerCase(Locale.ROOT).hashCode(),
                                LOGINS_PER_ADDRESS_AND_USER);
            case "/register":
                return limiter.tryAcquire("register:" + address, REGISTRATIONS_PER_ADDRESS);
            default:
                return 0;
        }
    }
}
