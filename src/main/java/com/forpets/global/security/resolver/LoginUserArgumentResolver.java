package com.forpets.global.security.resolver;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import org.springframework.core.MethodParameter;
import org.springframework.lang.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;

/**
 * @LoginUser가 붙은 컨트롤러 파라미터에 CurrentMember를 주입합니다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean hasCurrentMemberType = CurrentMember.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && hasCurrentMemberType;
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getPrincipal().equals("anonymousUser")
                || !(authentication.getPrincipal() instanceof CurrentMember currentMember)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return currentMember;
    }
}