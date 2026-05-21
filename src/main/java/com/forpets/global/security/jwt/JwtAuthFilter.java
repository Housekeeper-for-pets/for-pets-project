package com.forpets.global.security.jwt;

import com.forpets.domain.member.entity.MemberRole;
import com.forpets.global.security.PublicPathPatterns;
import com.forpets.global.security.dto.CurrentMember;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 Access Token을 검증하고 SecurityContext에 인증 정보를 저장합니다.
 * Redis 블랙리스트에 등록된 Access Token은 인증 처리하지 않습니다.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisService tokenRedisService;
    private final BearerTokenResolver bearerTokenResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method) && matches(PublicPathPatterns.AUTHENTICATED_GET, path)) {
            return false;
        }

        return matches(PublicPathPatterns.ANY_METHOD, path)
                || ("POST".equalsIgnoreCase(method) && matches(PublicPathPatterns.PUBLIC_POST, path))
                || ("GET".equalsIgnoreCase(method) && matches(PublicPathPatterns.PUBLIC_GET, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = bearerTokenResolver.resolve(request);

        try {
            if (StringUtils.hasText(token)
                    && !tokenRedisService.isBlacklisted(token)
                    && jwtTokenProvider.validateToken(token)) {

                Long memberId = jwtTokenProvider.getMemberId(token);
                MemberRole role = jwtTokenProvider.getRole(token);

                CurrentMember currentMember = new CurrentMember(memberId, role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentMember,
                                null,
                                List.of(new SimpleGrantedAuthority(role.getAuthority()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException exception) {
            request.setAttribute("jwtException", "EXPIRED_TOKEN");
        } catch (SecurityException | MalformedJwtException | SignatureException exception) {
            request.setAttribute("jwtException", "INVALID_TOKEN");
        } catch (UnsupportedJwtException | IllegalArgumentException exception) {
            request.setAttribute("jwtException", "INVALID_TOKEN");
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(String[] patterns, String path) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
