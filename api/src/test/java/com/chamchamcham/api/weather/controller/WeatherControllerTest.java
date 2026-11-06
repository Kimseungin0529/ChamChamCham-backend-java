package com.chamchamcham.api.weather.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.application.weather.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WeatherControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    @MockBean private FarmWeatherService farmWeatherService;
    @MockBean private TokenProvider tokenProvider;

    @Test @DisplayName("홈 날씨를 조회한다")
    void home() throws Exception {
        given(farmWeatherService.getHome(memberId, farmId)).willReturn(homeWeather());
        mockMvc.perform(get("/api/v1/weather/home").param("farmId",farmId.toString()).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.farmId",equalTo(farmId.toString())))
            .andExpect(jsonPath("$.data.condition.code",equalTo("CLEAR")))
            .andExpect(jsonPath("$.data.condition.text",equalTo("맑음")));
    }
    @Test @DisplayName("farmId 생략 시 서비스에 null이 전달된다")
    void omittedFarm() throws Exception {
        given(farmWeatherService.getHome(memberId,null)).willReturn(homeWeather());
        mockMvc.perform(get("/api/v1/weather/home").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk());
    }
    @Test @DisplayName("상세 날씨를 조회하고 condition은 code text 중첩 객체다")
    void detail() throws Exception {
        given(farmWeatherService.getDetail(memberId,farmId)).willReturn(detailWeather());
        mockMvc.perform(get("/api/v1/weather/detail").param("farmId",farmId.toString()).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current.condition.code",equalTo("CLEAR")))
            .andExpect(jsonPath("$.data.current.condition.text",equalTo("맑음")))
            .andExpect(jsonPath("$.data.forecast[0].dayOfWeek",equalTo("WEDNESDAY")))
            .andExpect(jsonPath("$.data.forecast[0].condition.code",equalTo("CLEAR")));
    }
    @Test @DisplayName("기록 날씨를 조회한다")
    void daily() throws Exception {
        given(farmWeatherService.getDaily(memberId,farmId,LocalDate.of(2026,7,15)))
            .willReturn(new DailyWeather(LocalDate.of(2026,7,15),WeatherCondition.CLEAR,25,29));
        mockMvc.perform(get("/api/v1/weather/daily").param("farmId",farmId.toString()).param("date","2026-07-15").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.date",equalTo("2026-07-15")))
            .andExpect(jsonPath("$.data.condition.code",equalTo("CLEAR")))
            .andExpect(jsonPath("$.data.minTemperature",equalTo(25)))
            .andExpect(jsonPath("$.data.maxTemperature",equalTo(29)));
    }
    @Test @DisplayName("기록 조회에 date가 없으면 400")
    void missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/weather/daily").param("farmId",farmId.toString()).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isBadRequest());
    }
    @Test @DisplayName("인증되지 않으면 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/weather/home"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code",equalTo("AUTH_001")));
    }
    @Test @DisplayName("상세 응답의 nullable 기상 값은 null이며 partial은 boolean과 배열로 직렬화된다")
    void nullableDetailJson() throws Exception {
        given(farmWeatherService.getDetail(memberId,farmId)).willReturn(new DetailWeather(farmId,"서울 중구 세종대로 110",
            LocalDateTime.of(2026,7,15,21,0),24,null,WeatherCondition.UNKNOWN,null,null,null,null,null,null,List.of(),PartialFailure.of()));
        var response = mockMvc.perform(get("/api/v1/weather/detail").param("farmId",farmId.toString()).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.observedAt",equalTo("2026-07-15T21:00:00")))
            .andExpect(jsonPath("$.data.current.temperature",equalTo(24)))
            .andExpect(jsonPath("$.data.current.condition.code",equalTo("UNKNOWN")))
            .andExpect(jsonPath("$.data.current.condition.text",equalTo("정보없음")))
            .andExpect(jsonPath("$.data.forecast",equalTo(List.of())))
            .andExpect(jsonPath("$.data.partial.degraded",equalTo(false)))
            .andExpect(jsonPath("$.data.partial.missing",equalTo(List.of())));
        for (String field : List.of("feelsLikeTemperature","minTemperature","maxTemperature","humidity","windSpeed","precipitationProbability","uvIndex")) {
            response.andExpect(jsonPath("$.data.current."+field).hasJsonPath())
                .andExpect(jsonPath("$.data.current."+field).value(org.hamcrest.Matchers.nullValue()));
        }
    }

    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id,null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }
    private HomeWeather homeWeather() {
        return new HomeWeather(farmId,24,WeatherCondition.CLEAR,25,29,LocalDateTime.of(2026,7,15,21,0),PartialFailure.of());
    }
    private DetailWeather detailWeather() {
        return new DetailWeather(farmId,"서울 중구 세종대로 110",LocalDateTime.of(2026,7,15,21,0),24,26,WeatherCondition.CLEAR,25,29,85,1.7,0,1,
            List.of(new DailyForecast(LocalDate.of(2026,7,15),WeatherCondition.CLEAR,25,29)),PartialFailure.of());
    }
}
