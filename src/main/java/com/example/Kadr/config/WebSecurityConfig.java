package com.example.Kadr.config;

import com.example.Kadr.service.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(GET,  "/", "/about", "/organizers", "/events/**",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/login", "/register", "/error").permitAll()

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