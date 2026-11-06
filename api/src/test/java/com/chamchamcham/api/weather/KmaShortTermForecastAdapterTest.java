package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class KmaShortTermForecastAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/VilageFcstInfoService_2.0";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WeatherLocation location = new WeatherLocation(37.5665,126.9780,"서울 중구 세종대로 110",null);
    private KmaShortTermForecastAdapter adapter;

    @Test
    @DisplayName("TMN TMX가 없으면 TMP가 여러 개 있어도 최저최고는 null이다")
    void test01() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2000")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("TMP", "24", "20260715", "2100"),
                        item("TMP", "23", "20260715", "2200"),
                        item("TMP", "22", "20260715", "2300"),
                        item("SKY", "1", "20260715", "2100"),
                        item("PTY", "0", "20260715", "2100")
                    )
                )
            );
        var result = adapter.fetchLatest(location);
        assertThat(result).isNotNull();
        var today = singleToday(result.getDailyForecasts());
        assertThat(today.getMinTemperature()).isNull();
        assertThat(today.getMaxTemperature()).isNull();
    }

    @Test
    @DisplayName("fetchTodayRange는 오늘 TMN TMX를 뽑는다")
    void test02() {
        setUp(LocalDateTime.of(2026, 7, 15, 10, 0));
        server.expect(requestTo(requestUri("20260715", "0200")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("TMN", "25.0", "20260715", "0600"),
                        item("TMX", "29.0", "20260715", "1500"),
                        item("SKY", "1", "20260715", "1200"),
                        item("PTY", "0", "20260715", "1200")
                    )
                )
            );
        var result = adapter.fetchTodayRange(location);
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(result.getMinTemperature()).isEqualTo(25);
        assertThat(result.getMaxTemperature()).isEqualTo(29);
    }

    @Test
    @DisplayName("fetchTodayRange에 오늘 항목이 없으면 null이다")
    void test03() {
        setUp(LocalDateTime.of(2026, 7, 15, 10, 0));
        server.expect(requestTo(requestUri("20260715", "0200")))
            .andRespond(withJson(bodyOf(item("TMN", "10.0", "20260716", "0600"))));
        var result = adapter.fetchTodayRange(location);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("currentSky는 당일 항목이 없어도 날짜 경계를 넘어 최근접 슬롯을 고른다")
    void test04() {
        setUp(LocalDateTime.of(2026, 7, 15, 23, 30));
        server.expect(requestTo(requestUri("20260715", "2300")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("SKY", "4", "20260716", "0000"),
                        item("PTY", "0", "20260716", "0000")
                    )
                )
            );
        var result = adapter.fetchLatest(location);
        assertThat(result).isNotNull();
        assertThat(result.getCurrentSky()).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    @DisplayName("items가 빈 리스트면 fetchLatest는 null을 반환한다")
    void test05() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2000")))
            .andRespond(withJson(emptyBodyOf("03")));
        var result = adapter.fetchLatest(location);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("dailyForecasts는 fcstDate별로 묶이고 날짜 오름차순이다")
    void test06() {
        setUp(LocalDateTime.of(2026, 7, 15, 21, 50));
        server.expect(requestTo(requestUri("20260715", "2000")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("TMP", "20", "20260717", "0900"),
                        item("TMP", "21", "20260715", "2100"),
                        item("TMP", "22", "20260716", "0900")
                    )
                )
            );
        var result = adapter.fetchLatest(location);
        assertThat(result).isNotNull();
        assertThat(result.getDailyForecasts().stream().map(DailyForecast::getDate).toList()).containsExactly(
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2026, 7, 16),
            LocalDate.of(2026, 7, 17)
        );
    }

    @Test
    @DisplayName("condition은 정오에 가장 가까운 fcstTime 슬롯에서 온다")
    void test07() {
        setUp(LocalDateTime.of(2026, 7, 15, 10, 0));
        server.expect(requestTo(requestUri("20260715", "0200")))
            .andRespond(
                withJson(
                    bodyOf(
                        item("SKY", "1", "20260715", "0900"),
                        item("PTY", "0", "20260715", "0900"),
                        item("SKY", "3", "20260715", "1100"),
                        item("PTY", "0", "20260715", "1100"),
                        item("SKY", "4", "20260715", "1500"),
                        item("PTY", "0", "20260715", "1500")
                    )
                )
            );
        var result = adapter.fetchTodayRange(location);
        assertThat(result).isNotNull();
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
    }
    private void setUp(LocalDateTime time) {
        Clock clock = Clock.fixed(time.atZone(SEOUL).toInstant(),SEOUL);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KmaApiClient client = new KmaApiClient(builder.build(),SERVICE_KEY);
        adapter = new KmaShortTermForecastAdapter(client,new KmaBaseTime(clock),properties(),clock);
    }
    private String requestUri(String date,String time) {
        return BASE_URL+"/getVilageFcst?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&numOfRows=1000&base_date="+date+"&base_time="+time+"&nx=60&ny=127";
    }
    private org.springframework.test.web.client.ResponseCreator withJson(String body) {
        return withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body);
    }
    private String emptyBodyOf(String code) {
        return """
            {"response":{"header":{"resultCode":"%s","resultMsg":"NODATA_ERROR"},
             "body":{"dataType":"JSON","items":{"item":[]},"pageNo":1,"numOfRows":1000,"totalCount":0}}}
            """.formatted(code).strip();
    }
    private String bodyOf(String... items) {
        return """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"dataType":"JSON","items":{"item":[%s]},
             "pageNo":1,"numOfRows":1000,"totalCount":%d}}}
            """.formatted(String.join(",", items), items.length).strip();
    }
    private KmaProperties properties() {
        return new KmaProperties(SERVICE_KEY,2000,2000,new KmaProperties.BaseUrl("https://weather.example.test/VilageFcstInfoService_2.0","https://weather.example.test/MidFcstInfoService","https://weather.example.test/AsosDalyInfoService","https://weather.example.test/LivingWthrIdxServiceV5"));
    }
    private String item(String category,String value,String date,String time) {
        return "{\"category\":\""+category+"\",\"fcstDate\":\""+date+"\",\"fcstTime\":\""+time+"\",\"fcstValue\":\""+value+"\"}";
    }
    private DailyForecast singleToday(List<DailyForecast> forecasts) {
        List<DailyForecast> today = forecasts.stream().filter(it -> it.getDate().equals(LocalDate.of(2026,7,15))).toList();
        assertThat(today).hasSize(1);
        return today.get(0);
    }

}
