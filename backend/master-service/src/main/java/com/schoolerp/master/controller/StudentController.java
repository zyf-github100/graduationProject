package com.schoolerp.master.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.api.PageResult;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.master.dto.StudentRecord;
import com.schoolerp.master.dto.StudentSaveRequest;
import com.schoolerp.master.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/summary")
    public ApiResponse<?> summary(HttpServletRequest request) {
        return ApiResponse.success(studentService.summary(), "查询成功", requestId(request));
    }

    @GetMapping("/options")
    public ApiResponse<?> options(HttpServletRequest request) {
        return ApiResponse.success(studentService.options(), "查询成功", requestId(request));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") long pageNo,
                               @RequestParam(defaultValue = "10") long pageSize,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String grade,
                               @RequestParam(required = false) String status,
                               HttpServletRequest request) {
        List<StudentRecord> allRecords = studentService.list(keyword, grade, status);
        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, allRecords.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, allRecords.size());
        PageResult<StudentRecord> pageResult = PageResult.of(allRecords.subList(fromIndex, toIndex), pageNo, pageSize, allRecords.size());
        return ApiResponse.success(pageResult, "查询成功", requestId(request));
    }

    @GetMapping("/{studentId}")
    public ApiResponse<?> detail(@PathVariable Long studentId, HttpServletRequest request) {
        return ApiResponse.success(studentService.detail(studentId), "查询成功", requestId(request));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody StudentSaveRequest requestBody, HttpServletRequest request) {
        return ApiResponse.success(studentService.create(requestBody), "学生档案创建成功", requestId(request));
    }

    @PutMapping("/{studentId}")
    public ApiResponse<?> update(@PathVariable Long studentId,
                                 @Valid @RequestBody StudentSaveRequest requestBody,
                                 HttpServletRequest request) {
        return ApiResponse.success(studentService.update(studentId, requestBody), "学生档案更新成功", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}
