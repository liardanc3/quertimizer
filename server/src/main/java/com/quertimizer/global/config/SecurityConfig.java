package com.quertimizer.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.filter.AccountRestrictionFilter;
import com.quertimizer.global.filter.ApiLoggingFilter;
import com.quertimizer.global.filter.CsrfCookieFilter;
import com.quertimizer.global.filter.CsrfCookieNormalizationFilter;
import com.quertimizer.global.handler.ApiExceptionHandler;
import com.quertimizer.global.properties.AppSecurityProperties;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepositoryPort userRepository;
    private final AccountRestrictionFilter accountRestrictionFilter;
    private final ApiLoggingFilter apiLoggingFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final CsrfCookieNormalizationFilter csrfCookieNormalizationFilter;
    private final AppSecurityProperties appSecurityProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-base-url:}")
    private String frontendBaseUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   TokenBasedRememberMeServices rememberMeServices,
                                                   CsrfTokenRepository csrfTokenRepository) throws Exception {
        return http.csrf(csrf -> csrf
                           .csrfTokenRepository(csrfTokenRepository)
                           .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                           .ignoringRequestMatchers(request ->
                                   HttpMethod.POST.matches(request.getMethod()) && "/session/me".equals(request.getServletPath())))
                   .cors(Customizer.withDefaults())
                   .httpBasic(AbstractHttpConfigurer::disable)
                   .formLogin(AbstractHttpConfigurer::disable)
                   .logout(AbstractHttpConfigurer::disable)
                   .exceptionHandling(exceptionHandling -> exceptionHandling
                           .authenticationEntryPoint((request, response, exception) ->
                                   writeSecurityExceptionResponse(
                                           response, HttpServletResponse.SC_UNAUTHORIZED,
                                           GlobalFailReason.AUTHENTICATION_REQUIRED.getMessage()
                                   ))
                           .accessDeniedHandler((request, response, exception) ->
                                   writeSecurityExceptionResponse(
                                           response, HttpServletResponse.SC_FORBIDDEN,
                                           GlobalFailReason.ACCESS_DENIED.getMessage()
                                   )))
                   .headers(headers -> headers.contentTypeOptions(Customizer.withDefaults()))
                   .oauth2Login(oauth2 -> oauth2
                           .redirectionEndpoint(redirection -> redirection.baseUri("/login/*"))
                           .successHandler((request, response, authentication) -> response.sendRedirect("/login/social/success"))
                           .failureHandler((request, response, exception) ->
                                   response.sendRedirect("/login/social/failure?provider=" + resolveSocialLoginProvider(request)))
                   )
                   .securityContext(context -> context.securityContextRepository(securityContextRepository))
                   .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                   .addFilterBefore(csrfCookieNormalizationFilter, CsrfFilter.class)
                   .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
                   .addFilterAfter(accountRestrictionFilter, SecurityContextHolderFilter.class)
                   .addFilterAfter(apiLoggingFilter, AccountRestrictionFilter.class)
                   .authorizeHttpRequests(auth -> auth
                           .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                           .requestMatchers(HttpMethod.GET,
                                   "/problems",
                                   "/problems/*",
                                   "/community/posts",
                                   "/community/posts/*",
                                   "/community/images/*",
                                   "/community/tags/suggestions",
                                   "/dashboard",
                                   "/submit-histories",
                                   "/ui-texts",
                                   "/ui-texts/*",
                                   "/ranks",
                                   "/profiles/**",
                                   "/login/social/**",
                                   "/oauth2/**"
                           ).permitAll()
                           .requestMatchers(HttpMethod.POST,
                                   "/login",
                                   "/signup",
                                   "/signup/send-code",
                                   "/signup/verify-code",
                                   "/duplicate-check/handle",
                                   "/duplicate-check/email",
                                   "/find-password/send-code",
                                   "/find-password/verify-code",
                                   "/find-password/reset",
                                   "/session/me"
                           ).permitAll()
                           .requestMatchers("/oauth2/**", "/login/*").permitAll()
                           .requestMatchers("/admin/auth-manage/**").hasRole(UserRole.ADMIN.name())
                           .requestMatchers("/admin/problem-sets/**", "/admin/problems", "/admin/problems/output-preview")
                           .hasRole(UserRole.ADMIN.name())
                           .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name())
                           .anyRequest().authenticated())
                   .build();
    }

    private void writeSecurityExceptionResponse(HttpServletResponse response, int status, String reason) throws IOException {
        // Spring Security 인증/인가 실패 응답을 공용 JSON 형식으로 변환
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiExceptionHandler.ExceptionResponse.reason(reason));
    }

    private String resolveSocialLoginProvider(HttpServletRequest request) {
        // failure redirect 시 프런트가 provider를 표시할 수 있게 마지막 path segment 사용
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "oauth2";
        }

        int lastSlashIndex = requestUri.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex == requestUri.length() - 1) {
            return "oauth2";
        }

        return requestUri.substring(lastSlashIndex + 1);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // 로그인 principal은 email을 사용하므로 email 기준 인증 사용자 조회
        return username -> userRepository.findByEmailIgnoreCase(username)
                .map(user ->
                        new org.springframework.security.core.userdetails.User(
                                user.getEmail(),
                                user.getPassword(),
                                AuthorityUtils.createAuthorityList("ROLE_" + user.getResolvedRole().name())
                        ))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    public TokenBasedRememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices rememberMeServices =
                new TokenBasedRememberMeServices(appSecurityProperties.getRememberMe().getKey(), userDetailsService);

        rememberMeServices.setCookieName("quertimizer-remember-me");
        rememberMeServices.setTokenValiditySeconds((int) appSecurityProperties.getRememberMe().getValidity().toSeconds());
        rememberMeServices.setUseSecureCookie(appSecurityProperties.getRememberMe().isSecure());
        return rememberMeServices;
    }

    @Bean
    public CookieSameSiteSupplier rememberMeCookieSameSiteSupplier() {
        return CookieSameSiteSupplier.of(appSecurityProperties.getRememberMe().getSameSite())
                .whenHasName("quertimizer-remember-me");
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        // 프론트엔드와 API 서브도메인이 함께 읽고 검증할 수 있는 CSRF 쿠키 구성
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> {
            cookie.path("/");
            cookie.sameSite("Lax");
            if (usesSharedQuertimizerCookieDomain()) {
                cookie.domain("quertimizer.com");
                cookie.secure(true);
            }
        });
        return repository;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        // Spring Session JDBC가 관리하는 HttpSession에 SecurityContext 저장
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new BCryptPasswordEncoder());

        return new DelegatingPasswordEncoder("pbkdf2", encoders);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        // DB 사용자 인증 provider 구성
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private boolean usesSharedQuertimizerCookieDomain() {
        // 운영 프론트엔드 도메인 여부에 따라 공유 쿠키 도메인 사용 여부 결정
        try {
            String host = URI.create(frontendBaseUrl).getHost();
            return host != null && (host.equals("quertimizer.com") || host.endsWith(".quertimizer.com"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

}
