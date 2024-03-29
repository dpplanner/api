package com.dp.dplanner.config.security;

import com.dp.dplanner.config.security.CustomAuthenticationEntryPoint;
import com.dp.dplanner.config.security.JwtAuthenticationFilter;
import com.dp.dplanner.config.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtTokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement((sessionManagement)
                        -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .headers((headers)
                        -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .formLogin(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests((authorizeHttpRequest)
                        -> authorizeHttpRequest
                        .requestMatchers(new AntPathRequestMatcher("/auth/**"))
                        .permitAll()
                        .requestMatchers("/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        .requestMatchers("/v3/**")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(
                        (exceptionHandling) -> exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint)
                );

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


}
