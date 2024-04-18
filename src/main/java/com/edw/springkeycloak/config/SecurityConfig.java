package com.edw.springkeycloak.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Log4j2
public class SecurityConfig {


    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;


    public SecurityConfig(RedisTemplate<String, Object> redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    @Order(1)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((authz) -> authz.requestMatchers("/test", "/favicon.ico")
                        .permitAll());

        http.oauth2Login((oauth2Login) -> oauth2Login
                .userInfoEndpoint((userInfo) -> userInfo
                        .oidcUserService(this.oidcUserService())
                        .userAuthoritiesMapper(grantedAuthoritiesMapper())


                ).successHandler(oauth2AuthenticationSuccessHandler()))
                .logout(logout -> logout.addLogoutHandler(oidcLogoutSuccessHandler())
                ).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));


        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler = new OidcAuthenticationSuccessHandler();
        oidcAuthenticationSuccessHandler.setHashOperations(redisTemplate.opsForHash());
        return oidcAuthenticationSuccessHandler;
    }

    @Bean
    public KeycloakLogoutHandler oidcLogoutSuccessHandler() {
        KeycloakLogoutHandler keycloakLogoutHandler =
                new KeycloakLogoutHandler(restTemplate);
        keycloakLogoutHandler.setHashOperations(redisTemplate.opsForHash());

        // Sets the location that the End-User's User Agent will be redirected to
        // after the logout has been performed at the Provider
        //keycloakLogoutHandler.("{baseUrl}");

        return keycloakLogoutHandler;
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default implementation for loading a user
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

            return oidcUser;
        };
    }


    private GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach((authority) -> {
                GrantedAuthority mappedAuthority;

                if (authority instanceof OidcUserAuthority) {
                    OidcUserAuthority userAuthority = (OidcUserAuthority) authority;
                    mappedAuthority = new OidcUserAuthority(
                            "OIDC_USER", userAuthority.getIdToken(), userAuthority.getUserInfo());
                } else if (authority instanceof OAuth2UserAuthority) {
                    OAuth2UserAuthority userAuthority = (OAuth2UserAuthority) authority;
                    mappedAuthority = new OAuth2UserAuthority(
                            "OAUTH2_USER", userAuthority.getAttributes());
                } else {
                    mappedAuthority = authority;
                }

                mappedAuthorities.add(mappedAuthority);
            });

            return mappedAuthorities;
        };
    }



//    @Order(2)
//    @Bean
//    public SecurityFilterChain client(HttpSecurity http) throws Exception {
//        return http
//                .authorizeHttpRequests((authz) -> authz.requestMatchers("/favicon.ico")
//                        //.requestMatchers("/login").authenticated())
//                        .hasRole("USER")
//                        .anyRequest().authenticated())
//              .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()))
//                .build();
//    }

}