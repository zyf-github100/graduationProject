package com.schoolerp.master.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.master.service.TeacherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/master/teacher")
public class TeacherPortalController {
    private final TeacherService teacherService;

    public TeacherPortalController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile(@RequestParam(defaultValue = "T2020018") String teacherNo, HttpServletRequest request) {
        return ApiResponse.success(teacherService.teacherProfile(teacherNo), "查询成功", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}
