package com.quertimizer.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.ratelimit.InMemoryGlobalRateLimiter;
import com.quertimizer.global.util.ClientIpResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_ANONYMOUS_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_AUTHENTICATED_SHORT_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GlobalRateLimitFilter")
class GlobalRateLimitFilterTest {

    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final GlobalRateLimitFilter filter = new GlobalRateLimitFilter(
            new ObjectMapper(), clientIpResolver, new InMemoryGlobalRateLimiter()
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("성공 (제한 미만 요청 통과)")
        void successWhenRequestCountBelowLimit() throws Exception {
            // given
            when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
            MockHttpServletRequest request = request("GET", "/dashboard");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("실패 (비로그인 IP 기준 제한 초과)")
        void failWhenAnonymousIpLimitExceeded() throws Exception {
            // given
            when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
            for (int count = 0; count < HTTP_ANONYMOUS_SHORT_LIMIT; count++) {
                filter.doFilter(request("GET", "/dashboard"), new MockHttpServletResponse(), mock(FilterChain.class));
            }
            MockHttpServletRequest request = request("GET", "/dashboard");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentAsString()).contains("요청이 너무 많습니다");
        }

        @Test
        @DisplayName("실패 (인증 사용자 기준 제한 초과)")
        void failWhenAuthenticatedUserLimitExceeded() throws Exception {
            // given
            authenticate("solver@quertimizer.com", "ROLE_USER");
            for (int count = 0; count < HTTP_AUTHENTICATED_SHORT_LIMIT; count++) {
                filter.doFilter(request("GET", "/dashboard"), new MockHttpServletResponse(), mock(FilterChain.class));
            }
            MockHttpServletRequest request = request("GET", "/dashboard");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentAsString()).contains("요청이 너무 많습니다");
        }

        @Test
        @DisplayName("성공 (사용자별 key 분리)")
        void successWhenAuthenticatedUserKeysAreDifferent() throws Exception {
            // given
            authenticate("solver-a@quertimizer.com", "ROLE_USER");
            for (int count = 0; count < HTTP_AUTHENTICATED_SHORT_LIMIT; count++) {
                filter.doFilter(request("GET", "/dashboard"), new MockHttpServletResponse(), mock(FilterChain.class));
            }
            authenticate("solver-b@quertimizer.com", "ROLE_USER");
            MockHttpServletRequest request = request("GET", "/dashboard");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("성공 (관리자 제한 적용)")
        void successWhenAdminLimitApplied() throws Exception {
            // given
            authenticate("admin@quertimizer.com", "ROLE_ADMIN");
            for (int count = 0; count < HTTP_AUTHENTICATED_SHORT_LIMIT + 1; count++) {
                filter.doFilter(request("GET", "/admin"), new MockHttpServletResponse(), mock(FilterChain.class));
            }
            MockHttpServletRequest request = request("GET", "/admin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("성공 (OPTIONS와 WebSocket handshake 제외)")
        void successWhenExcludedRequest() throws Exception {
            // given
            MockHttpServletRequest optionsRequest = request("OPTIONS", "/dashboard");
            MockHttpServletResponse optionsResponse = new MockHttpServletResponse();
            FilterChain optionsChain = mock(FilterChain.class);
            MockHttpServletRequest webSocketRequest = request("GET", "/ws/session");
            MockHttpServletResponse webSocketResponse = new MockHttpServletResponse();
            FilterChain webSocketChain = mock(FilterChain.class);

            // when
            filter.doFilter(optionsRequest, optionsResponse, optionsChain);
            filter.doFilter(webSocketRequest, webSocketResponse, webSocketChain);

            // then
            verify(optionsChain).doFilter(optionsRequest, optionsResponse);
            verify(webSocketChain).doFilter(webSocketRequest, webSocketResponse);
        }
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static void authenticate(String principal, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, "password", AuthorityUtils.createAuthorityList(role)
                )
        );
    }
}
