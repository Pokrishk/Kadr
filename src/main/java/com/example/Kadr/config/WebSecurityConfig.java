package com.example.Kadr.config;

import com.example.Kadr.service.AppUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var requestHandler = new CsrfTokenRequestAttributeHandler();
        var cookieRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(GET,  "/", "/about", "/organizers", "/events/**",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/login", "/register", "/error").permitAll()
                        .requestMatchers("/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(POST, "/events/*/reviews").authenticated()
                        .requestMatchers(POST, "/events/reviews/*/delete").authenticated()
                        .requestMatchers(GET,  "/cart").authenticated()
                        .requestMatchers(POST, "/cart/**").authenticated()

                        .requestMatchers("/profile", "/cart", "/orders").hasAnyRole("USER","ORGANIZER","ADMIN")
                        .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(e -> e.accessDeniedPage("/access-denied"));

        http.addFilterAfter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                CsrfToken csrf = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
                if (csrf != null) { csrf.getToken(); }
                chain.doFilter(req, res);
            }
        }, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authProvider(AppUserDetailsService uds, PasswordEncoder enc) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(enc);
        return p;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}