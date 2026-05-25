package com.guan.rag.module.token.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.token.response.TokenCostOverviewResponse;
import com.guan.rag.module.token.service.TokenCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/token-cost")
@RequiredArgsConstructor
public class TokenCostController {

    private final TokenCostService tokenCostService;

    @GetMapping("/overview")
    public ApiResponse<TokenCostOverviewResponse> overview() {
        return ApiResponse.success(tokenCostService.getOverview());
    }
}
