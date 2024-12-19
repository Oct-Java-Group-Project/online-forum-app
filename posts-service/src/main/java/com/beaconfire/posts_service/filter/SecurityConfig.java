//package com.beaconfire.posts_service.filter;
//
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//public class SecurityConfig {
//
//  @Bean
//  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//      http
//          .csrf(csrf -> csrf.disable())
//          .authorizeHttpRequests(auth -> auth
//              .anyRequest().authenticated()
//          )
//          .addFilterBefore(new GatewayHeaderValidationFilter(), UsernamePasswordAuthenticationFilter.class);
//
//      return http.build();
//  }
//}
//
