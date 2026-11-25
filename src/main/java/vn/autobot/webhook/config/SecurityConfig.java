package vn.autobot.webhook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.Header;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import vn.autobot.webhook.service.McpRateLimiter;

import java.util.LinkedHashMap;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // JSON endpoints of the signed-in user; the rest of /api/** is mock endpoints
    private static final String[] SESSION_APIS = {"/api/logs/**", "/api/configs/**"};

    private static final RequestMatcher MOCK_ENDPOINTS = new AntPathRequestMatcher("/api/@*/**");

    // CSP only reports (browser console): the templates still have inline scripts, styles and handlers
    private static final List<Header> PAGE_HEADERS = List.of(
            new Header("Referrer-Policy", "strict-origin-when-cross-origin"),
            new Header("Permissions-Policy", "camera=(), geolocation=(), microphone=(), payment=(), usb=()"),
            new Header("Content-Security-Policy-Report-Only", "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://code.jquery.com https://cdn.datatables.net; "
                    + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://cdn.datatables.net; "
                    + "font-src 'self' data: https://cdnjs.cloudflare.com; img-src 'self' data:; "
                    + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'"));

    private final UserDetailsService userDetailsService;
    private final McpRateLimiter rateLimiter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // Only paths that do not act on the session: mock endpoints, MCP (Bearer token), SockJS (checks Origin)
            .csrf().ignoringAntMatchers("/api/@*/**", "/mcp/**", "/ws/**")
                .and()
            .addFilterBefore(new AuthRateLimitFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
                .antMatchers(SESSION_APIS).authenticated()
                .antMatchers("/api/**").permitAll() // Allow all webhook API requests
                .antMatchers("/mcp/**").permitAll() // MCP clients authenticate with their own Bearer token
                .antMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/swagger.json", "/swagger-ui", "/swagger-ui/**", "/error").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
            .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
                .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
                .and()
            .headers()
                // Mock bodies are user-written: run them in an opaque origin, away from the signed-in pages.
                // Added, not set: a CSP from the endpoint's own headers must not replace it.
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(MOCK_ENDPOINTS,
                        (request, response) -> {
                            // Async mock responses pass here twice
                            if (!response.getHeaders("Content-Security-Policy").contains("sandbox")) {
                                response.addHeader("Content-Security-Policy", "sandbox");
                            }
                        }))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(MOCK_ENDPOINTS),
                        new StaticHeadersWriter(PAGE_HEADERS)))
                .frameOptions().sameOrigin(); // SockJS fallback transports use iframes
    }

    /** 401 for JSON endpoints, the login page for everything else. */
    private static AuthenticationEntryPoint authenticationEntryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        for (String path : SESSION_APIS) {
            entryPoints.put(new AntPathRequestMatcher(path), new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        }
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return entryPoint;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
