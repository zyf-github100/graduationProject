package com.schoolerp.master.controller;

import com.schoolerp.master.service.StudentService;
import com.schoolerp.master.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/master")
public class MasterInternalController {
    private final StudentService studentService;
    private final TeacherService teacherService;

    public MasterInternalController(StudentService studentService, TeacherService teacherService) {
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    @GetMapping("/students/profile")
    public Map<String, Object> studentProfile(@RequestParam(defaultValue = "202501001") String studentNo) {
        return studentService.studentProfile(studentNo);
    }

    @GetMapping("/teachers/profile")
    public Map<String, Object> teacherProfile(@RequestParam(defaultValue = "T2020018") String teacherNo) {
        return teacherService.teacherProfile(teacherNo);
    }
}
