package com.quertimizer.config;

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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final ApiLoggingFilter apiLoggingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   TokenBasedRememberMeServices rememberMeServices) throws Exception {

        // 세션 기반 인증, 로그인 유지, API 로깅 필터를 Security 흐름에 연결
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                .addFilterAfter(apiLoggingFilter, SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/signup").permitAll()
                        .anyRequest().permitAll());

        return http.build();
    }

    // 유저 인증정보 세션 저장
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        return username -> userRepository.findById(username)
                .map(user ->
                        new org.springframework.security.core.userdetails.User(
                                user.getUserId(),
                                user.getPassword(),
                                AuthorityUtils.NO_AUTHORITIES
                        ))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    // 로그인 유지 쿠키 설정
    @Bean
    public TokenBasedRememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices rememberMeServices =
                new TokenBasedRememberMeServices("quertimizer-remember-me-key", userDetailsService);

        rememberMeServices.setCookieName("quertimizer-remember-me");
        rememberMeServices.setTokenValiditySeconds((int) Duration.ofDays(180).toSeconds());
        return rememberMeServices;
    }

    // 서버에서 SHA512 한번 더 적용
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

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

}
