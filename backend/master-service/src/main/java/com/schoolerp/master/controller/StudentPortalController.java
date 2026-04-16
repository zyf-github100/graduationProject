package com.schoolerp.master.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.master.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/master/student")
public class StudentPortalController {
    private final StudentService studentService;

    public StudentPortalController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile(@RequestParam(defaultValue = "202501001") String studentNo, HttpServletRequest request) {
        return ApiResponse.success(studentService.studentProfile(studentNo), "查询成功", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}
