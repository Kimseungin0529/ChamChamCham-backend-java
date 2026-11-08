package com.chamchamcham.api.weather.controller;

import java.util.Objects;
import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.weather.dto.WeatherResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.weather.FarmWeatherService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {
    private final FarmWeatherService farmWeatherService;

    public WeatherController(FarmWeatherService farmWeatherService) { this.farmWeatherService = Objects.requireNonNull(farmWeatherService, "farmWeatherService"); }

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<WeatherResponses.HomeResponse>> home(
        @AuthenticationPrincipal String memberId, @RequestParam(required = false) UUID farmId) {
        var result = farmWeatherService.getHome(parseMemberId(memberId), farmId);
        return ResponseEntity.ok(ApiResponse.ok(WeatherResponses.HomeResponse.from(result)));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<WeatherResponses.DetailResponse>> detail(
        @AuthenticationPrincipal String memberId, @RequestParam(required = false) UUID farmId) {
        var result = farmWeatherService.getDetail(parseMemberId(memberId), farmId);
        return ResponseEntity.ok(ApiResponse.ok(WeatherResponses.DetailResponse.from(result)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<WeatherResponses.DailyResponse>> daily(
        @AuthenticationPrincipal String memberId, @RequestParam(required = false) UUID farmId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var result = farmWeatherService.getDaily(parseMemberId(memberId), farmId, date);
        return ResponseEntity.ok(ApiResponse.ok(WeatherResponses.DailyResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
