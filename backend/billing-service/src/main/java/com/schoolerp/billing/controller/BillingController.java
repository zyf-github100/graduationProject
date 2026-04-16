package com.schoolerp.billing.controller;

import com.schoolerp.billing.service.BillingService;
import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.api.PageResult;
import com.schoolerp.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/bills/summary")
    public ApiResponse<?> summary(HttpServletRequest request) {
        return ApiResponse.success(billingService.summary(), "查询成功", requestId(request));
    }

    @GetMapping("/student/overview")
    public ApiResponse<?> studentOverview(HttpServletRequest request) {
        return ApiResponse.success(billingService.studentOverview(), "查询成功", requestId(request));
    }

    @GetMapping("/bills")
    public ApiResponse<?> bills(@RequestParam(defaultValue = "1") long pageNo,
                                @RequestParam(defaultValue = "10") long pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String status,
                                HttpServletRequest request) {
        List<Map<String, Object>> allBills = billingService.list(keyword, status);
        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, allBills.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, allBills.size());
        return ApiResponse.success(PageResult.of(allBills.subList(fromIndex, toIndex), pageNo, pageSize, allBills.size()), "查询成功", requestId(request));
    }

    @GetMapping("/bills/{billId}")
    public ApiResponse<?> detail(@PathVariable Long billId, HttpServletRequest request) {
        return ApiResponse.success(billingService.detail(billId), "查询成功", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}
