package com.cfs.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
        csrfTokenRequestAttributeHandler.setCsrfRequestAttributeName(null);
        http
                .cors(cors->cors.configurationSource((corsConfigurationSource)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/auth/register", "/auth/me", "/login", "/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form->form
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> response.setStatus(200))
                        .failureHandler((request, response, exception) -> response.setStatus(401))
                )
                .logout(logout->logout
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID" , "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
                )
                .userDetailsService(customUserDetailsService)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
        ;
                return http.build();
    }



}
