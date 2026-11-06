package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.*;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RecordFeedbackKmaWeatherAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/VilageFcstInfoService_2.0";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private RecordFeedbackKmaWeatherAdapter adapter;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026,7,11,10,0);

    @Test
    @DisplayName("실황에 강수가 없으면 동네예보 SKY로 현재 하늘상태를 보완하고 일별 값을 집계한다")
    void test01() {
        setUp(FIXED_TIME);
        server.expect(requestTo(ncstUri("20260711", "0900")))
            .andRespond(withJson(ncstBodyOf("14.2", "0")));
        server.expect(requestTo(vilageFcstUri("20260711", "0800")))
            .andRespond(withJson(THREE_DAYS_BODY));
        var result = adapter.fetch(37.5665, 126.9780, 7);
        assertThat(result.getCurrent().getTemperatureC()).isEqualTo(14);
        assertThat(result.getCurrent().getSkyCondition()).isEqualTo("맑음") ;
        assertThat(result.getCurrent().getObservedAt()).isEqualTo(LocalDateTime.of(2026, 7, 11, 9, 0));
        assertThat(result.getSource()).isEqualTo("KMA_SHORT_TERM");
        assertThat(result.getForecastDays().stream().map(day -> day.getDate().toString()).toList())
            .containsExactly("2026-07-11", "2026-07-12", "2026-07-13");
        var day0 = result.getForecastDays().get(0);
        assertThat(day0.getRainfallMm()).isEqualByComparingTo("22.5");
        assertThat(day0.getRainProbabilityPct()).isEqualTo(80);
        assertThat(day0.getMaxTemperatureC()).isEqualByComparingTo("31");
        assertThat(day0.getMinTemperatureC()).isEqualByComparingTo("24");
        assertThat(day0.getHumidityPct()).isEqualByComparingTo("88");
        assertThat(day0.getWindSpeedMs()).isEqualByComparingTo("9.1");
        assertThat(day0.getRiskFlags()).containsExactly("RAIN", "HEAVY_RAIN", "HIGH_HUMIDITY", "HOT", "STRONG_WIND");
        assertThat(result.getForecastDays().get(1).getRiskFlags()).containsExactly("RAIN");
        assertThat(result.getForecastDays().get(2).getRiskFlags()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("실황에 강수형태가 있으면 동네예보 SKY보다 우선한다")
    void test02() {
        setUp(FIXED_TIME);
        server.expect(requestTo(ncstUri("20260711", "0900")))
            .andRespond(withJson(ncstBodyOf("14.2", "1")));
        server.expect(requestTo(vilageFcstUri("20260711", "0800")))
            .andRespond(withJson(THREE_DAYS_BODY)) ;
        var result = adapter.fetch(37.5665, 126.9780, 1);
        assertThat(result.getCurrent().getSkyCondition()).isEqualTo("비");
    }

    @Test
    @DisplayName("정성 강수량은 숫자로 만들지 않고 강수확률과 강수형태만 위험 신호에 반영한다")
    void test03() {
        setUp(FIXED_TIME);
        server.expect(requestTo(ncstUri("20260711", "0900")))
            .andRespond(withJson(ncstBodyOf("14.2", "0")));
        server.expect(requestTo(vilageFcstUri("20260711", "0800")))
            .andRespond(withJson(QUALITATIVE_PCP_BODY));
        var result = adapter.fetch(37.5665, 126.9780, 1);
        assertThat(result.getForecastDays()).hasSize(1);
        assertThat(result.getForecastDays().get(0).getRainfallMm()).isNull();
        assertThat(result.getForecastDays().get(0).getRainProbabilityPct()).isEqualTo(70);
        assertThat(result.getForecastDays().get(0).getRiskFlags()).containsExactly("RAIN");
    }

    @Test
    @DisplayName("동네예보 자료가 없어도 예보 없이 현재 날씨는 반환한다")
    void test04() {
        setUp(FIXED_TIME);
        server.expect(requestTo(ncstUri("20260711", "0900")))
            .andRespond(withJson(ncstBodyOf("14.2", "0")));
        server.expect(requestTo(vilageFcstUri("20260711", "0800")))
            .andRespond(withJson(emptyBodyOf("03")));
        var result = adapter.fetch(37.5665, 126.9780, 7);
        assertThat(result.getCurrent().getTemperatureC()).isEqualTo(14);
        assertThat(result.getCurrent().getSkyCondition()).isEqualTo("정보없음");
        assertThat(result.getForecastDays()).isEmpty();
    }

    @Test
    @DisplayName("limitDays 범위를 벗어나면 조회 없이 예외를 던진다")
    void test05() {
        setUp(FIXED_TIME);
        assertThatThrownBy(() -> adapter.fetch(37.5665, 126.9780, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.fetch(37.5665, 126.9780, 8))
            .isInstanceOf(IllegalArgumentException.class);
    }
    private void setUp(LocalDateTime time) {
        Clock clock = Clock.fixed(time.atZone(SEOUL).toInstant(),SEOUL);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KmaApiClient client = new KmaApiClient(builder.build(),SERVICE_KEY);
        adapter = new RecordFeedbackKmaWeatherAdapter(new KmaCurrentObservationAdapter(client,new KmaBaseTime(clock),properties()),client,new KmaBaseTime(clock),properties(),clock);
    }
    private String ncstUri(String date,String time) {
        return BASE_URL+"/getUltraSrtNcst?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&numOfRows=100&base_date="+date+"&base_time="+time+"&nx=60&ny=127";
    }
    private String vilageFcstUri(String date,String time) {
        return BASE_URL+"/getVilageFcst?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&numOfRows=2000&base_date="+date+"&base_time="+time+"&nx=60&ny=127";
    }
    private String ncstBodyOf(String temperature,String pty) {
        return "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"items\":{\"item\":[{\"baseDate\":\"20260711\",\"baseTime\":\"0900\",\"category\":\"T1H\",\"obsrValue\":\""+temperature+"\"},{\"baseDate\":\"20260711\",\"baseTime\":\"0900\",\"category\":\"PTY\",\"obsrValue\":\""+pty+"\"}]}}}}";
    }
    private org.springframework.test.web.client.ResponseCreator withJson(String body) {
        return withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body);
    }
    private String emptyBodyOf(String code) {
        return "{\"response\":{\"header\":{\"resultCode\":\""+code+"\",\"resultMsg\":\"NODATA_ERROR\"},\"body\":{\"items\":{\"item\":[]}}}}";
    }
    private String bodyOf(String... items) {
        return "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"items\":{\"item\":["+String.join(",",items)+"]}}}}";
    }
    private KmaProperties properties() {
        return new KmaProperties(SERVICE_KEY,2000,2000,new KmaProperties.BaseUrl("https://weather.example.test/VilageFcstInfoService_2.0","https://weather.example.test/MidFcstInfoService","https://weather.example.test/AsosDalyInfoService","https://weather.example.test/LivingWthrIdxServiceV5"));
    }
    private static final String THREE_DAYS_BODY = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"dataType":"JSON","items":{"item":[
               {"category":"SKY","fcstDate":"20260711","fcstTime":"0900","fcstValue":"1"},
               {"category":"TMN","fcstDate":"20260711","fcstTime":"0600","fcstValue":"24"},
               {"category":"TMX","fcstDate":"20260711","fcstTime":"1500","fcstValue":"31"},
               {"category":"POP","fcstDate":"20260711","fcstTime":"1200","fcstValue":"80"},
               {"category":"REH","fcstDate":"20260711","fcstTime":"1200","fcstValue":"88"},
               {"category":"WSD","fcstDate":"20260711","fcstTime":"1200","fcstValue":"9.1"},
               {"category":"PTY","fcstDate":"20260711","fcstTime":"1200","fcstValue":"1"},
               {"category":"PCP","fcstDate":"20260711","fcstTime":"1200","fcstValue":"22.5mm"},
               {"category":"TMN","fcstDate":"20260712","fcstTime":"0600","fcstValue":"20"},
               {"category":"TMX","fcstDate":"20260712","fcstTime":"1500","fcstValue":"27"},
               {"category":"POP","fcstDate":"20260712","fcstTime":"1200","fcstValue":"40"},
               {"category":"PTY","fcstDate":"20260712","fcstTime":"1200","fcstValue":"1"},
               {"category":"REH","fcstDate":"20260712","fcstTime":"1200","fcstValue":"70"},
               {"category":"WSD","fcstDate":"20260712","fcstTime":"1200","fcstValue":"3.5"},
               {"category":"PCP","fcstDate":"20260712","fcstTime":"1200","fcstValue":"강수없음"},
               {"category":"TMN","fcstDate":"20260713","fcstTime":"0600","fcstValue":"21"},
               {"category":"TMX","fcstDate":"20260713","fcstTime":"1500","fcstValue":"29"},
               {"category":"POP","fcstDate":"20260713","fcstTime":"1200","fcstValue":"20"},
               {"category":"PTY","fcstDate":"20260713","fcstTime":"1200","fcstValue":"0"},
               {"category":"REH","fcstDate":"20260713","fcstTime":"1200","fcstValue":"60"},
               {"category":"WSD","fcstDate":"20260713","fcstTime":"1200","fcstValue":"2.5"},
               {"category":"PCP","fcstDate":"20260713","fcstTime":"1200","fcstValue":"0"}
             ]},"pageNo":1,"numOfRows":1000,"totalCount":21}}}
        """;
    private static final String QUALITATIVE_PCP_BODY = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"dataType":"JSON","items":{"item":[
               {"category":"TMN","fcstDate":"20260711","fcstTime":"0600","fcstValue":"22"},
               {"category":"TMX","fcstDate":"20260711","fcstTime":"1500","fcstValue":"28"},
               {"category":"POP","fcstDate":"20260711","fcstTime":"1200","fcstValue":"70"},
               {"category":"PTY","fcstDate":"20260711","fcstTime":"1200","fcstValue":"0"},
               {"category":"PCP","fcstDate":"20260711","fcstTime":"1200","fcstValue":"1.0mm 미만"}
             ]},"pageNo":1,"numOfRows":1000,"totalCount":5}}}
        """;

}
