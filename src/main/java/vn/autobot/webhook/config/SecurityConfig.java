package vn.autobot.webhook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // Only paths that do not act on the session: mock endpoints, MCP (Bearer token), SockJS (checks Origin)
            // and the H2 console (own session token)
            .csrf().ignoringAntMatchers("/api/@*/**", "/mcp/**", "/ws/**", "/h2-console/**")
                .and()
            .authorizeRequests()
                .antMatchers("/api/**").permitAll() // Allow all webhook API requests
                .antMatchers("/mcp/**").permitAll() // MCP clients authenticate with their own Bearer token
                .antMatchers("/register", "/login", "/css/**", "/js/**", "/swagger.json", "/swagger-ui", "/swagger-ui/**", "/swagger/**", "/error").permitAll()
                .antMatchers("/admin/**", "/h2-console/**").hasRole("ADMIN")
                .anyRequest().authenticated()
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
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new AntPathRequestMatcher("/api/@*/**"),
                        (request, response) -> {
                            // Async mock responses pass here twice
                            if (!response.getHeaders("Content-Security-Policy").contains("sandbox")) {
                                response.addHeader("Content-Security-Policy", "sandbox");
                            }
                        }))
                .frameOptions().sameOrigin(); // For H2 console
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
