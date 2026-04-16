package com.schoolerp.auth.controller;

import com.schoolerp.auth.dto.LoginRequest;
import com.schoolerp.auth.dto.TokenRefreshRequest;
import com.schoolerp.auth.service.DemoAuthService;
import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.common.security.CurrentUser;
import com.schoolerp.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final DemoAuthService demoAuthService;

    public AuthController(DemoAuthService demoAuthService) {
        this.demoAuthService = demoAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(demoAuthService.login(request), "登录成功", requestId(servletRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestHeader("Authorization") String authorization, HttpServletRequest servletRequest) {
        String accessToken = tokenFromHeader(authorization);
        demoAuthService.logout(accessToken);
        return ApiResponse.success(java.util.Map.of("success", true), "退出成功", requestId(servletRequest));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody TokenRefreshRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(demoAuthService.refresh(request.refreshToken()), "刷新成功", requestId(servletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest servletRequest) {
        return ApiResponse.success(demoAuthService.me(currentUser), "查询成功", requestId(servletRequest));
    }

    @GetMapping("/me/menus")
    public ApiResponse<?> menus(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest servletRequest) {
        return ApiResponse.success(demoAuthService.menus(currentUser), "查询成功", requestId(servletRequest));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<?> permissions(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest servletRequest) {
        return ApiResponse.success(demoAuthService.permissions(currentUser), "查询成功", requestId(servletRequest));
    }

    private String tokenFromHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, 401, "缺少有效的访问令牌");
        }
        return authorization.substring(7);
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}
