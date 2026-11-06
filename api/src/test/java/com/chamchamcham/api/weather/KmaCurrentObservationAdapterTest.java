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

class KmaCurrentObservationAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/VilageFcstInfoService_2.0";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WeatherLocation location = new WeatherLocation(37.5665,126.9780,"서울 중구 세종대로 110",null);
    private KmaCurrentObservationAdapter adapter;

    @Test
    @DisplayName("T1H REH WSD를 정상 파싱하고 PTY=0이면 강수형태는 null이다")
    void test01() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("T1H", "24.4"),
                        item("REH", "85"),
                        item("WSD", "1.7"),
                        item("PTY", "0")
                    )
                )
            );
        var result = adapter.fetch(location);
        assertThat(result.getTemperature()).isEqualTo(24);
        assertThat(result.getHumidity()).isEqualTo(85);
        assertThat(result.getWindSpeed()).isEqualTo(1.7);
        assertThat(result.getPrecipitationType()).isNull();
    }

    @Test
    @DisplayName("PTY=1이면 RAIN이다")
    void test02() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(withJson(bodyOf(item("T1H", "24.4"), item("PTY", "1"))));
        var result = adapter.fetch(location);
        assertThat(result.getPrecipitationType()).isEqualTo(WeatherCondition.RAIN);
    }

    @Test
    @DisplayName("PTY가 0이 아닌 알 수 없는 코드면 강수형태는 null이다")
    void test03() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(withJson(bodyOf(item("T1H", "24.4"), item("PTY", "9"))));
        var result = adapter.fetch(location);
        assertThat(result.getPrecipitationType()).isNull();
    }

    @Test
    @DisplayName("T1H가 없으면 WEATHER_002를 던진다")
    void test04() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(withJson(bodyOf(item("REH", "85"))));
        var exception = assertThrows(BusinessException.class, () -> adapter.fetch(location));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("items가 빈 리스트면 WEATHER_002를 던진다")
    void test05() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(withJson(emptyBodyOf("03")));
        var exception = assertThrows(BusinessException.class, () -> adapter.fetch(location));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("observedAt은 baseDate와 baseTime을 합쳐 파싱한다")
    void test06() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2100")))
            .andRespond(withJson(bodyOf(item("T1H", "24.4", "20260715", "2100"))));
        var result = adapter.fetch(location);
        assertThat(result.getObservedAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 21, 0));
    }
    private void setUp(LocalDateTime time) {
        Clock clock = Clock.fixed(time.atZone(SEOUL).toInstant(),SEOUL);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KmaApiClient client = new KmaApiClient(builder.build(),SERVICE_KEY);
        adapter = new KmaCurrentObservationAdapter(client,new KmaBaseTime(clock),properties());
    }
    private String requestUri(String date,String time) {
        return BASE_URL+"/getUltraSrtNcst?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&numOfRows=100&base_date="+date+"&base_time="+time+"&nx=60&ny=127";
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
    private String item(String category,String value) { return item(category,value,"20260715","2100"); }
    private String item(String category,String value,String date,String time) {
        return "{\"baseDate\":\""+date+"\",\"baseTime\":\""+time+"\",\"category\":\""+category+"\",\"obsrValue\":\""+value+"\"}";
    }

}
