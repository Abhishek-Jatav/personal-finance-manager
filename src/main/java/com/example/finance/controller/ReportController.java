package com.example.finance.controller;

import com.example.finance.dto.MonthlyReportResponse;
import com.example.finance.dto.YearlyReportResponse;
import com.example.finance.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Routes under /api/reports. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** GET /api/reports/monthly/{year}/{month} - income and expenses by category for one month. */
    @GetMapping("/monthly/{year}/{month}")
    public MonthlyReportResponse monthly(@PathVariable int year, @PathVariable int month) {
        return reportService.monthly(year, month);
    }

    /** GET /api/reports/yearly/{year} - income and expenses by category for one year. */
    @GetMapping("/yearly/{year}")
    public YearlyReportResponse yearly(@PathVariable int year) {
        return reportService.yearly(year);
    }
}
