package com.schoolerp.academic.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "master-service", path = "/internal/master")
public interface MasterProfileClient {
    @GetMapping("/students/profile")
    Map<String, Object> studentProfile(@RequestParam("studentNo") String studentNo);

    @GetMapping("/teachers/profile")
    Map<String, Object> teacherProfile(@RequestParam("teacherNo") String teacherNo);
}
