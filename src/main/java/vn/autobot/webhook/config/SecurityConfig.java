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
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/**").permitAll() // Allow all webhook API requests
                .antMatchers("/mcp/**").permitAll() // MCP clients authenticate with their own Bearer token
                .antMatchers("/register", "/login", "/css/**", "/js/**", "/swagger.json", "/swagger-ui", "/swagger-ui/**", "/swagger/**").permitAll()
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
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
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
