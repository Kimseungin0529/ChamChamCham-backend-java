package com.chamchamcham.api.security;

import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final ObjectProvider<MemberRepository> memberRepositoryProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, ObjectProvider<MemberRepository> memberRepositoryProvider) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.memberRepositoryProvider = Objects.requireNonNull(memberRepositoryProvider, "memberRepositoryProvider");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        var token = resolveToken(request);
        if (token != null && tokenProvider.validateToken(token) && tokenProvider.isAccessToken(token)) {
            var memberId = tokenProvider.getMemberId(token);
            var memberRepository = memberRepositoryProvider.getIfAvailable();
            if (memberRepository != null && memberRepository.existsById(memberId)) {
                SecurityContextHolder.getContext().setAuthentication(tokenProvider.getAuthentication(token));
            }
        }
        filterChain.doFilter(request, response);
    }

    @Nullable
    private String resolveToken(HttpServletRequest request) {
        var bearer = request.getHeader("Authorization");
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : null;
    }
}
