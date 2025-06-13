package com.icegreen.greenmail.marketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(MemberService memberService) {
        return email -> memberService.getMemberByEmail(email)
                .map(member -> new org.springframework.security.core.userdetails.User(
                        member.getEmail(),
                        member.getPasswordHash(),
                        new ArrayList<>())) // Empty authorities list for now
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS configuration
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs or SPAs
            .authorizeHttpRequests(authz -> authz
                .antMatchers(HttpMethod.POST, "/api/marketing/members/register").permitAll()
                .antMatchers(HttpMethod.POST, "/api/marketing/members/login").permitAll()
                // Allow Swagger/OpenAPI docs if they are added later
                // .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .antMatchers("/api/marketing/**").authenticated() // Secure other marketing API endpoints
                .anyRequest().permitAll() // Allow other requests (e.g. to non-API paths if any, or actuator) - adjust as needed
            )
            // Using HttpBasic for simple auth, can be replaced with formLogin or JWT later
            .httpBasic(httpBasic -> {});
            // For formLogin:
            // .formLogin(formLogin -> formLogin
            //     .loginProcessingUrl("/api/marketing/members/login") // Spring Security handles this endpoint
            //     .usernameParameter("email") // if your LoginRequest uses "email"
            //     .passwordParameter("password")
            //     .successHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK)) // Simple 200 OK
            //     .failureHandler(new SimpleUrlAuthenticationFailureHandler()) // Default redirects to /login?error
            // );
            // .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://localhost:3000")); // Example: allow frontend dev server
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Member-Id")); // Include custom headers if any
        configuration.setAllowCredentials(true); // If you need cookies/sessions
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
