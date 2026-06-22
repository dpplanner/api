package com.dp.dplanner.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtTokenProvider tokenProvider;

    // 웹(브라우저) 허용 오리진. application-*.yml의 app.cors.allowed-origins 로 콤마구분 지정.
    // 기본값: 로컬 개발(flutter run -d chrome) 허용. 운영 웹 도메인은 yml에 추가.
    @Value("${app.cors.allowed-origins:http://localhost:*,https://localhost:*}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorizeHttpRequest) -> authorizeHttpRequest
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling((exceptionHandling) -> exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint));

//                .oauth2Login((oauth2Login) ->
//                    oauth2Login
//                            .authorizationEndpoint((authorization)->
//                                    authorization.baseUri("/oauth2/authorization"))
//                            .redirectionEndpoint((redirection)->
//                                    redirection.baseUri("/*/oauth2/code/*"))
//                            .userInfoEndpoint((userInfoEndpointConfig) ->
//                                    userInfoEndpointConfig.userService(customOAuth2UserService))
//                            .successHandler(successHandler)
//                );
        http.addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 패턴(*) 지원 + 자격증명 허용을 위해 AllowedOriginPatterns 사용
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
