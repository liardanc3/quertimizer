package com.quertimizer.global.config;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.filter.AccountRestrictionFilter;
import com.quertimizer.global.filter.ApiLoggingFilter;
import com.quertimizer.user.application.port.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
                                                   TokenBasedRememberMeServices rememberMeServices) throws Exception {
        // 세션, remember-me, API 로그 필터 구성
        return http.csrf(AbstractHttpConfigurer::disable)
                   .cors(Customizer.withDefaults())
                   .httpBasic(AbstractHttpConfigurer::disable)
                   .formLogin(AbstractHttpConfigurer::disable)
                   .logout(AbstractHttpConfigurer::disable)
                   .oauth2Login(oauth2 -> oauth2
                           // 소셜 로그인 완료 후에는 controller에서 세션 저장, 접근 기록, redirect를 한 번에 처리한다.
                           .redirectionEndpoint(redirection -> redirection.baseUri("/login/*"))
                           .successHandler((request, response, authentication) -> response.sendRedirect("/login/social/success"))
                           .failureHandler((request, response, exception) ->
                                   response.sendRedirect("/login/social/failure?provider=" + resolveSocialLoginProvider(request)))
                   )
                   .securityContext(context -> context.securityContextRepository(securityContextRepository))
                   .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                   .addFilterAfter(accountRestrictionFilter, SecurityContextHolderFilter.class)
                   .addFilterAfter(apiLoggingFilter, AccountRestrictionFilter.class)
                   .authorizeHttpRequests(auth -> auth
                           // 로그인, 회원가입, OAuth2 진입점은 비로그인 상태에서도 접근 가능해야 한다.
                           .requestMatchers("/login", "/login/*", "/logout", "/signup", "/signup/*", "/oauth2/**").permitAll()
                           .requestMatchers("/admin/auth-manage/**").hasRole(UserRole.ADMIN.name())
                           .requestMatchers("/admin/problem-sets/**", "/admin/problems", "/admin/problems/output-preview")
                           .hasAnyRole(UserRole.ADMIN.name(), UserRole.PROBLEM_GENERATOR.name())
                           .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name())
                           .anyRequest().permitAll())
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
        // remember-me 서비스 생성
        TokenBasedRememberMeServices rememberMeServices =
                new TokenBasedRememberMeServices("quertimizer-remember-me-key", userDetailsService);

        // 로그인 유지 쿠키 설정
        rememberMeServices.setCookieName("quertimizer-remember-me");
        rememberMeServices.setTokenValiditySeconds((int) Duration.ofDays(180).toSeconds());
        return rememberMeServices;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {

        // Spring Session JDBC가 관리하는 HttpSession에 SecurityContext를 저장한다.
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호 인코더 생성
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                // 클라이언트에서 1차 SHA-512 처리한 값을 서버에서 한 번 더 SHA-512로 감싸 저장한다.
                return Sha512DigestUtils.shaHex(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                // 비밀번호 일치 여부 확인
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
