package com.schoolerp.auth.service;

import com.schoolerp.auth.dto.CurrentUserResponse;
import com.schoolerp.auth.dto.LoginRequest;
import com.schoolerp.auth.dto.LoginResponse;
import com.schoolerp.auth.dto.MenuItemDto;
import com.schoolerp.auth.dto.PermissionResponse;
import com.schoolerp.auth.security.JwtService;
import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.common.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DemoAuthService {
    private final AuthUser adminUser = new AuthUser(
            2001001L,
            "admin.wang",
            "123456",
            "王老师",
            "SCHOOL_ADMIN",
            List.of("广州软件学院管理员", "教务管理中心平台主管"),
            31001L,
            "教务管理中心",
            List.of("dashboard:view", "master:student:read", "master:student:write", "workflow:task:approve", "billing:bill:export", "notify:inbox:view"),
            List.of("CAMPUS:主校区", "GRADE:2025级", "GRADE:2024级", "GRADE:2023级"),
            List.of("student:export", "billing:export"),
            List.of("ADMIN_WEB")
    );

    private final AuthUser studentUser = new AuthUser(
            202501001L,
            "202501001",
            "123456",
            "林嘉禾",
            "STUDENT",
            List.of("2025级软件工程1班", "本科生"),
            41001L,
            "软件工程学院",
            List.of("student:home:view", "student:schedule:view", "student:scores:view", "student:billing:view", "student:notice:view", "student:profile:view"),
            List.of("SELF"),
            List.of(),
            List.of("STUDENT_APP", "ADMIN_WEB")
    );

    private final AuthUser teacherUser = new AuthUser(
            3002001L,
            "teacher.chen",
            "123456",
            "陈老师",
            "TEACHER",
            List.of("2025级软件工程1班辅导员", "大学英语教师"),
            32018L,
            "基础教学部",
            List.of(
                    "teacher:home:view",
                    "teacher:schedule:view",
                    "teacher:classes:view",
                    "teacher:attendance:write",
                    "teacher:grades:write",
                    "teacher:notice:view",
                    "teacher:profile:view"
            ),
            List.of("SELF", "CLASS:2025级软件工程1班", "CLASS:2024级软件工程3班"),
            List.of("teacher:schedule:export", "teacher:attendance:export", "teacher:grades:export"),
            List.of("TEACHER_WEB", "ADMIN_WEB")
    );

    private final Map<String, AuthUser> usersByUsername = Map.of(
            adminUser.username(), adminUser,
            studentUser.username(), studentUser,
            teacherUser.username(), teacherUser
    );

    private final JwtService jwtService;
    private final long refreshTokenTtlSeconds;
    private final Map<String, SessionState> accessSessions = new ConcurrentHashMap<>();
    private final Map<String, String> refreshIndex = new ConcurrentHashMap<>();

    public DemoAuthService(JwtService jwtService,
                           @Value("${school-erp.security.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        this.jwtService = jwtService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public LoginResponse login(LoginRequest request) {
        AuthUser authUser = usersByUsername.get(request.username());
        if (authUser == null || !authUser.password().equals(request.password())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, 401, "账号或密码错误");
        }

        String clientType = resolveClientType(request.clientType(), authUser);
        if (!authUser.allowedClientTypes().contains(clientType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, 403, "当前账号不允许从该客户端类型登录");
        }

        return createSession(authUser);
    }

    public void logout(String accessToken) {
        SessionState sessionState = accessSessions.remove(accessToken);
        if (sessionState != null) {
            refreshIndex.remove(sessionState.refreshToken());
        }
    }

    public LoginResponse refresh(String refreshToken) {
        String accessToken = refreshIndex.get(refreshToken);
        if (accessToken == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, 401, "刷新令牌无效或已失效");
        }

        SessionState sessionState = accessSessions.get(accessToken);
        if (sessionState == null || sessionState.expiredAt().isBefore(Instant.now())) {
            accessSessions.remove(accessToken);
            refreshIndex.remove(refreshToken);
            throw new BusinessException(ResultCode.UNAUTHORIZED, 401, "刷新令牌已过期，请重新登录");
        }

        logout(accessToken);
        return createSession(sessionState.authUser());
    }

    public boolean isAccessTokenActive(String accessToken) {
        SessionState sessionState = accessSessions.get(accessToken);
        return sessionState != null && sessionState.expiredAt().isAfter(Instant.now());
    }

    public CurrentUserResponse me(CurrentUser currentUser) {
        AuthUser authUser = requireUser(currentUser.username());
        return new CurrentUserResponse(
                currentUser.userId(),
                currentUser.username(),
                currentUser.displayName(),
                currentUser.userType(),
                currentUser.roles(),
                authUser.orgUnitId(),
                currentUser.orgUnit()
        );
    }

    public List<MenuItemDto> menus(CurrentUser currentUser) {
        AuthUser authUser = requireUser(currentUser.username());
        return switch (authUser.userType()) {
            case "STUDENT" -> studentMenus();
            case "TEACHER" -> teacherMenus();
            default -> adminMenus();
        };
    }

    public PermissionResponse permissions(CurrentUser currentUser) {
        AuthUser authUser = requireUser(currentUser.username());
        return new PermissionResponse(authUser.permissions(), authUser.dataScopes(), authUser.exportPermissions());
    }

    private AuthUser requireUser(String username) {
        AuthUser authUser = usersByUsername.get(username);
        if (authUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, 401, "当前登录态无效");
        }
        return authUser;
    }

    private String resolveClientType(String clientType, AuthUser authUser) {
        if (clientType != null && !clientType.isBlank()) {
            return clientType;
        }
        return switch (authUser.userType()) {
            case "STUDENT" -> "STUDENT_APP";
            case "TEACHER" -> "TEACHER_WEB";
            default -> "ADMIN_WEB";
        };
    }

    private LoginResponse createSession(AuthUser authUser) {
        CurrentUser currentUser = new CurrentUser(
                authUser.userId(),
                authUser.username(),
                authUser.displayName(),
                authUser.userType(),
                authUser.roles(),
                authUser.orgUnit()
        );

        String accessToken = jwtService.generateAccessToken(currentUser);
        String refreshToken = "ref_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiredAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        SessionState sessionState = new SessionState(accessToken, refreshToken, expiredAt, authUser);
        accessSessions.put(accessToken, sessionState);
        refreshIndex.put(refreshToken, accessToken);

        return new LoginResponse(accessToken, refreshToken, 7200L, authUser.userId(), authUser.displayName(), authUser.userType());
    }

    private List<MenuItemDto> studentMenus() {
        return List.of(
                new MenuItemDto(1L, "学生首页", "/student/home", "StudentHomePage", "House", List.of()),
                new MenuItemDto(2L, "我的课表", "/student/schedule", "StudentSchedulePage", "Calendar", List.of()),
                new MenuItemDto(3L, "我的成绩", "/student/scores", "StudentScoresPage", "DocumentChecked", List.of()),
                new MenuItemDto(4L, "缴费中心", "/student/payments", "StudentPaymentPage", "Money", List.of()),
                new MenuItemDto(5L, "通知公告", "/student/notices", "StudentNoticePage", "Bell", List.of()),
                new MenuItemDto(6L, "个人信息", "/student/profile", "StudentProfilePage", "UserFilled", List.of())
        );
    }

    private List<MenuItemDto> teacherMenus() {
        return List.of(
                new MenuItemDto(101L, "教师首页", "/teacher/home", "TeacherHomePage", "House", List.of()),
                new MenuItemDto(102L, "我的课表", "/teacher/schedule", "TeacherSchedulePage", "Calendar", List.of()),
                new MenuItemDto(103L, "授课班级", "/teacher/classes", "TeacherClassesPage", "UserFilled", List.of()),
                new MenuItemDto(104L, "考勤登记", "/teacher/attendance", "TeacherAttendancePage", "DocumentChecked", List.of()),
                new MenuItemDto(105L, "成绩录入", "/teacher/grades", "TeacherGradesPage", "DocumentChecked", List.of()),
                new MenuItemDto(106L, "通知中心", "/teacher/notices", "TeacherNoticePage", "Bell", List.of()),
                new MenuItemDto(107L, "个人信息", "/teacher/profile", "TeacherProfilePage", "Setting", List.of())
        );
    }

    private List<MenuItemDto> adminMenus() {
        return List.of(
                new MenuItemDto(1L, "系统首页", "/dashboard", "DashboardPage", "House", List.of()),
                new MenuItemDto(2L, "基础数据", "/master", "Layout", "UserFilled", List.of(
                        new MenuItemDto(21L, "学生档案", "/students", "StudentListPage", null, List.of())
                )),
                new MenuItemDto(3L, "教务管理", "/academic", "AcademicManagementPage", "Calendar", List.of()),
                new MenuItemDto(4L, "审批中心", "/approvals", "ApprovalPage", "DocumentChecked", List.of()),
                new MenuItemDto(5L, "收费管理", "/billing", "Layout", "Money", List.of(
                        new MenuItemDto(51L, "账单管理", "/billing/bills", "BillingPage", null, List.of())
                )),
                new MenuItemDto(6L, "消息收件箱", "/notifications/inbox", "NotificationInboxPage", "Bell", List.of()),
                new MenuItemDto(7L, "个人中心", "/settings/profile", "ProfilePage", "Setting", List.of())
        );
    }

    private record SessionState(String accessToken, String refreshToken, Instant expiredAt, AuthUser authUser) {
    }

    private record AuthUser(
            Long userId,
            String username,
            String password,
            String displayName,
            String userType,
            List<String> roles,
            Long orgUnitId,
            String orgUnit,
            List<String> permissions,
            List<String> dataScopes,
            List<String> exportPermissions,
            List<String> allowedClientTypes
    ) {
    }
}
