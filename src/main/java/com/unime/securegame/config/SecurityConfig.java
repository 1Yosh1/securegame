package com.unime.securegame.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {

    private final boolean requireAdminKey;
    private final AdminApiKeyFilter adminApiKeyFilter;

    public SecurityConfig(@Value("${securegame.admin.require-key:false}") boolean requireAdminKey,
                          AdminApiKeyFilter adminApiKeyFilter) {
        this.requireAdminKey = requireAdminKey;
        this.adminApiKeyFilter = adminApiKeyFilter;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.securityContext(context -> context.securityContextRepository(securityContextRepository()));
        http.csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        http.addFilterBefore(adminApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers("/", "/index.html", "/game.css", "/game.js", "/minigames.js",
                            "/campaign.js", "/favicon.ico", "/api/csrf", "/actuator/health",
                            "/api/auth/register", "/api/auth/login", "/api/auth/login-token",
                            "/api/auth/verify", "/api/auth/forgot-password", "/api/auth/reset-password")
                    .permitAll();
            if (requireAdminKey) {
                authorize.requestMatchers("/api/scenarios", "/api/scenarios/**", "/api/risk/train",
                                "/api/risk/coach/**", "/api/players", "/api/players/**",
                                "/api/classrooms/**").hasRole("ADMIN");
            } else {
                authorize.requestMatchers(HttpMethod.GET, "/api/scenarios", "/api/scenarios/*",
                                "/api/scenarios/generate", "/api/risk/dataset/csv").permitAll();
            }
            authorize.requestMatchers(HttpMethod.POST, "/api/risk/evaluate").hasAnyRole("TEACHER", "ADMIN");
            authorize.requestMatchers(HttpMethod.GET, "/api/players")
                    .hasAnyRole("TEACHER", "ADMIN");
            authorize.requestMatchers(HttpMethod.GET, "/api/players/*")
                    .hasAnyRole("USER", "STUDENT", "SOLO", "TEACHER", "ADMIN");
            authorize.requestMatchers(HttpMethod.POST, "/api/players")
                    .hasAnyRole("USER", "STUDENT", "SOLO", "TEACHER", "ADMIN");
            authorize.requestMatchers(HttpMethod.PUT, "/api/players/*/score")
                    .hasRole("TEACHER");
            authorize.requestMatchers(HttpMethod.DELETE, "/api/players/**").hasRole("ADMIN");
            if (!requireAdminKey) {
                authorize.requestMatchers(HttpMethod.POST, "/api/scenarios", "/api/scenarios/*",
                                "/api/risk/train", "/api/risk/coach/**")
                        .hasRole("TEACHER");
                authorize.requestMatchers(HttpMethod.PUT, "/api/scenarios/*").hasRole("TEACHER");
                authorize.requestMatchers(HttpMethod.DELETE, "/api/scenarios/*", "/api/risk/coach/**")
                        .hasRole("TEACHER");
            }
            authorize.requestMatchers("/api/auth/update-role", "/api/auth/logout", "/api/auth/progress",
                            "/api/auth/classroom/**", "/api/classrooms/**",
                            "/api/room/**", "/api/multiplayer/**", "/api/risk/**", "/api/game/**",
                            "/api/simulation/**", "/api/phishing/**", "/ws/**")
                    .authenticated();
        });
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        super.configure(http);
        try {
            setLoginView(http, "/index.html");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure Vaadin login view", e);
        }
    }
}
