package com.derbenev.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Весь сайт требует HTTP Basic на каждый запрос (нет сессионной аутентификации через
     * cookie), поэтому дефолтная CSRF-защита Spring Security только мешает: она рассчитана
     * на браузерные cookie-сессии, а не на explicit-credential auth, и без неё все POST-формы
     * и REST-запросы (curl, fetch()) получали 401 даже с верным логином/паролем.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
