package com.quertimizer.config;

import com.quertimizer.constant.UserRole;
import com.quertimizer.filter.AccountRestrictionFilter;
import com.quertimizer.filter.ApiLoggingFilter;
import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final AccountRestrictionFilter accountRestrictionFilter;
    private final ApiLoggingFilter apiLoggingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   TokenBasedRememberMeServices rememberMeServices,
                                                   SocialOAuth2SuccessHandler socialOAuth2SuccessHandler,
                                                   SocialOAuth2FailureHandler socialOAuth2FailureHandler) throws Exception {

        // 세션, remember-me, API 로그 필터 구성
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirection -> redirection.baseUri("/login/*"))
                        .successHandler(socialOAuth2SuccessHandler)
                        .failureHandler(socialOAuth2FailureHandler)
                )
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                .addFilterAfter(accountRestrictionFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(apiLoggingFilter, AccountRestrictionFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/*", "/logout", "/signup", "/oauth2/**").permitAll()
                        .requestMatchers("/admin/auth-manage/**").hasRole(UserRole.ADMIN.name())
                        .requestMatchers("/admin/problem-sets/**", "/admin/problems")
                        .hasAnyRole(UserRole.ADMIN.name(), UserRole.PROBLEM_GENERATOR.name())
                        .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name())
                        .anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        // userId 기준 인증 사용자 조회
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
                new TokenBasedRememberMeServices("quertimizer-remember-me-key", userDetailsService);

        // 로그인 유지 쿠키 설정
        rememberMeServices.setCookieName("quertimizer-remember-me");
        rememberMeServices.setTokenValiditySeconds((int) Duration.ofDays(180).toSeconds());
        return rememberMeServices;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return Sha512DigestUtils.shaHex(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
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
