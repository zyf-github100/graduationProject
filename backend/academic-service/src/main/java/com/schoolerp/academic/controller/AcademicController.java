package com.schoolerp.academic.controller;

import com.schoolerp.academic.service.AcademicOverviewService;
import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academic")
public class AcademicController {
    private final AcademicOverviewService academicOverviewService;

    public AcademicController(AcademicOverviewService academicOverviewService) {
        this.academicOverviewService = academicOverviewService;
    }

    @GetMapping("/overview")
    public ApiResponse<?> overview(HttpServletRequest request) {
        return success(academicOverviewService.overview(), request);
    }

    @GetMapping("/student/home")
    public ApiResponse<?> studentHome(HttpServletRequest request) {
        return success(academicOverviewService.studentHome(), request);
    }

    @GetMapping("/student/schedule")
    public ApiResponse<?> studentSchedule(HttpServletRequest request) {
        return success(academicOverviewService.studentSchedule(), request);
    }

    @GetMapping("/student/scores")
    public ApiResponse<?> studentScores(HttpServletRequest request) {
        return success(academicOverviewService.studentScores(), request);
    }

    @GetMapping("/teacher/home")
    public ApiResponse<?> teacherHome(HttpServletRequest request) {
        return success(academicOverviewService.teacherHome(), request);
    }

    @GetMapping("/teacher/schedule")
    public ApiResponse<?> teacherSchedule(HttpServletRequest request) {
        return success(academicOverviewService.teacherSchedule(), request);
    }

    @GetMapping("/teacher/classes")
    public ApiResponse<?> teacherClasses(HttpServletRequest request) {
        return success(academicOverviewService.teacherClasses(), request);
    }

    @GetMapping("/teacher/attendance")
    public ApiResponse<?> teacherAttendance(HttpServletRequest request) {
        return success(academicOverviewService.teacherAttendance(), request);
    }

    @GetMapping("/teacher/grades")
    public ApiResponse<?> teacherGrades(HttpServletRequest request) {
        return success(academicOverviewService.teacherGrades(), request);
    }

    private ApiResponse<?> success(Object data, HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return ApiResponse.success(data, "查询成功", requestId == null ? "req_unknown" : requestId.toString());
    }
}
