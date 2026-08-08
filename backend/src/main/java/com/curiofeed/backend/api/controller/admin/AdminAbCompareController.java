package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.domain.service.AbComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ab-compare")
public class AdminAbCompareController {

    private final AbComparisonService abComparisonService;

    public AdminAbCompareController(AbComparisonService abComparisonService) {
        this.abComparisonService = abComparisonService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> compare(
            @RequestParam(defaultValue = "v3.0") String promptVersionA,
            @RequestParam(defaultValue = "v2.0") String promptVersionB) {
        Map<String, Object> comparison = abComparisonService.compareVersions(promptVersionA, promptVersionB);
        return ResponseEntity.ok(comparison);
    }
}
