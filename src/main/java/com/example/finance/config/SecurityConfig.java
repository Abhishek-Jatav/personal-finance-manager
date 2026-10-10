package com.example.finance.config;

import com.example.finance.security.RestSecurityHandlers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           RestSecurityHandlers handlers,
                                           SecurityContextRepository securityContextRepository,
                                           @Value("${app.security.csrf-enabled:true}") boolean csrfEnabled,
                                           @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled)
            throws Exception {

        http.securityContext(sc -> sc.securityContextRepository(securityContextRepository));
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers));

        if (csrfEnabled) {
            // The server sends the token in the XSRF-TOKEN cookie on every response.
            // Clients copy it into the X-XSRF-TOKEN header on POST/PUT/DELETE requests.
            CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
            requestHandler.setCsrfRequestAttributeName(null);
            http.csrf(csrf -> {
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                csrf.csrfTokenRequestHandler(requestHandler);
                csrf.ignoringRequestMatchers(antMatcher("/api/auth/register"), antMatcher("/api/auth/login"));
                if (h2ConsoleEnabled) {
                    csrf.ignoringRequestMatchers(PathRequest.toH2Console());
                }
            });
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        if (h2ConsoleEnabled) {
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        }

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(
                    antMatcher("/api/auth/register"),
                    antMatcher("/api/auth/login"),
                    antMatcher("/api/auth/logout"),
                    antMatcher("/actuator/health"),
                    antMatcher("/actuator/health/**"),
                    antMatcher("/error")).permitAll();
            if (h2ConsoleEnabled) {
                auth.requestMatchers(PathRequest.toH2Console()).permitAll();
            }
            auth.anyRequest().authenticated();
        });

        return http.build();
    }
}
