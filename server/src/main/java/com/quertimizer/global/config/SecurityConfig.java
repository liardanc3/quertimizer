package com.quertimizer.global.config;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.filter.AccountRestrictionFilter;
import com.quertimizer.global.filter.ApiLoggingFilter;
import com.quertimizer.global.filter.CsrfCookieFilter;
import com.quertimizer.global.properties.AppSecurityProperties;
import com.quertimizer.user.application.port.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final AccountRestrictionFilter accountRestrictionFilter;
    private final ApiLoggingFilter apiLoggingFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final AppSecurityProperties appSecurityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   TokenBasedRememberMeServices rememberMeServices) throws Exception {
        return http.csrf(csrf -> csrf
                           .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                   .cors(Customizer.withDefaults())
                   .httpBasic(AbstractHttpConfigurer::disable)
                   .formLogin(AbstractHttpConfigurer::disable)
                   .logout(AbstractHttpConfigurer::disable)
                   .exceptionHandling(exceptionHandling -> exceptionHandling
                           .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                           .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
                   .headers(headers -> headers.contentTypeOptions(Customizer.withDefaults()))
                   .oauth2Login(oauth2 -> oauth2
                           .redirectionEndpoint(redirection -> redirection.baseUri("/login/*"))
                           .successHandler((request, response, authentication) -> response.sendRedirect("/login/social/success"))
                           .failureHandler((request, response, exception) ->
                                   response.sendRedirect("/login/social/failure?provider=" + resolveSocialLoginProvider(request)))
                   )
                   .securityContext(context -> context.securityContextRepository(securityContextRepository))
                   .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
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
                           .hasAnyRole(UserRole.ADMIN.name(), UserRole.PROBLEM_GENERATOR.name())
                           .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name())
                           .anyRequest().authenticated())
                   .build();
    }

    private String resolveSocialLoginProvider(HttpServletRequest request) {
        // failure redirect 시 프런트가 어떤 provider에서 실패했는지 표시할 수 있게 마지막 path segment를 사용한다.
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

        // 로그인 principal은 email을 사용하므로 email 기준으로 인증 사용자를 조회한다.
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
    public SecurityContextRepository securityContextRepository() {

        // Spring Session JDBC가 관리하는 HttpSession에 SecurityContext를 저장한다.
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

}
