package group.g.graduation.backend.Security.config;

import group.g.graduation.backend.Security.filter.JwtAuthenticationFilter;
import group.g.graduation.backend.Security.jwt.JwtTokenProvider;
import group.g.graduation.backend.Security.oauth2.CustomOAuth2UserService;
import group.g.graduation.backend.Security.oauth2.OAuth2AuthenticationFailureHandler;
import group.g.graduation.backend.Security.oauth2.OAuth2AuthenticationSuccessHandler;
import group.g.graduation.backend.Security.service.UserDetailsServiceImpl;
import group.g.graduation.backend.common.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
        @Lazy JwtTokenProvider tokenProvider,
        @Lazy UserDetailsServiceImpl userDetailsService,
        PasswordEncoder passwordEncoder,
        @Lazy CustomOAuth2UserService customOAuth2UserService,
        @Lazy OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
        @Lazy OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
        RateLimitFilter rateLimitFilter
    ) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/**",
                    "/api/security/**",
                    "/api/email/test/**",  // Email testing endpoints
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/uploads/**",  // Allow public access to uploaded files
                    "/api/admin/users/create-admin"  // IMPORTANT: Must come BEFORE /api/admin/**
                ).permitAll()
                // Admin endpoints require ADMIN role (MUST come after specific permitAll)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Role management endpoints
                .requestMatchers("/api/roles/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/api/permissions/**").hasRole("ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Custom authentication entry point for API requests
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint())
            )
            // OAuth2 Login Configuration
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorize")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            // Rate Limit Filter (before authentication)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // JWT Authentication Filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }
    
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = 
            new org.springframework.web.cors.CorsConfiguration();
        
        // Allow localhost origins for development
        configuration.setAllowedOriginPatterns(java.util.List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "*"
        ));
        
        // Allow ALL HTTP methods including PATCH
        configuration.setAllowedMethods(java.util.List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(java.util.List.of(
            "Authorization", "Content-Type", "Accept", "X-Requested-With",
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(java.util.List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = 
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("✅ CORS configured: Methods=[GET,POST,PUT,PATCH,DELETE,OPTIONS]");
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, 
                org.springframework.security.core.AuthenticationException authException) -> {
            
            String requestUri = request.getRequestURI();
            String acceptHeader = request.getHeader("Accept");
            
            log.debug("🔒 Authentication failed for: {} - Accept: {}", requestUri, acceptHeader);
            
            // For API requests, return JSON error instead of HTML redirect
            if (requestUri.startsWith("/api/") || 
                (acceptHeader != null && acceptHeader.contains("application/json"))) {
                
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                
                var errorResponse = new java.util.HashMap<String, Object>();
                errorResponse.put("error", "Unauthorized");
                errorResponse.put("message", "Authentication required");
                errorResponse.put("timestamp", java.time.Instant.now().toString());
                errorResponse.put("path", requestUri);
                errorResponse.put("status", 401);
                
                ObjectMapper mapper = new ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(errorResponse));
                response.getWriter().flush();
                
            } else {
                // For regular browser requests, redirect to login
                response.sendRedirect("/login");
            }
        };
    }
}