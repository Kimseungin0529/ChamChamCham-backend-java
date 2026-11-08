package com.chamchamcham.api.security;

import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.MemberRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private TokenProvider tokenProvider;
    @Mock private MemberRepository memberRepository;
    @Mock private ObjectProvider<MemberRepository> memberRepositoryProvider;
    @Mock private FilterChain chain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        authentication = new UsernamePasswordAuthenticationToken(memberId.toString(), null, java.util.List.of());
        filter = new JwtAuthenticationFilter(tokenProvider, memberRepositoryProvider);
    }

    @AfterEach
    void clearSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("valid access token authenticates existing member")
    void validAccessTokenAuthenticatesExistingMember() throws Exception {
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(memberRepository);
        request.addHeader("Authorization", "Bearer access-token");
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.isAccessToken("access-token")).thenReturn(true);
        when(tokenProvider.getMemberId("access-token")).thenReturn(memberId);
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(tokenProvider.getAuthentication("access-token")).thenReturn(authentication);

        filter.doFilter(request, response, chain);

        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("valid token for deleted member stays unauthenticated")
    void validTokenForDeletedMemberStaysUnauthenticated() throws Exception {
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(memberRepository);
        request.addHeader("Authorization", "Bearer access-token");
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.isAccessToken("access-token")).thenReturn(true);
        when(tokenProvider.getMemberId("access-token")).thenReturn(memberId);
        when(memberRepository.existsById(memberId)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("valid token stays unauthenticated when member repository is unavailable")
    void validTokenStaysUnauthenticatedWhenMemberRepositoryIsUnavailable() throws Exception {
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(null);
        request.addHeader("Authorization", "Bearer access-token");
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.isAccessToken("access-token")).thenReturn(true);
        when(tokenProvider.getMemberId("access-token")).thenReturn(memberId);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(chain).doFilter(request, response);
    }
}
