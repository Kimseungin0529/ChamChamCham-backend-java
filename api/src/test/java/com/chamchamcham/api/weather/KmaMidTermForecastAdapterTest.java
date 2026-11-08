package com.chamchamcham.api.weather;

import com.chamchamcham.application.weather.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class KmaMidTermForecastAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/MidFcstInfoService";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WeatherLocation seoulLocation = new WeatherLocation(37.5665,126.9780,"서울 중구 세종대로 110",null);

    @Test
    @DisplayName("tmFc가 오늘 06시면 n=4로 taMin4와 wf4Pm을 읽는다")
    void test01() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 19);
        server.expect(requestTo(taUri("11B10101", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("taMin4", "24", "taMax4", "30"))));
        server.expect(requestTo(landUri("11B00000", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("wf4Pm", "흐리고 비"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        assertThat(result.getMinTemperature()).isEqualTo(24);
        assertThat(result.getMaxTemperature()).isEqualTo(30);
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.RAIN);
        server.verify();
    }

    @Test
    @DisplayName("tmFc가 어제 18시면 n=5로 taMin5와 wf5Pm을 읽는다")
    void test02() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 0, 5));
        var date = LocalDate.of(2026, 7, 19);
        server.expect(requestTo(taUri("11B10101", "202607141800")))
            .andRespond(withJson(itemsBody(Map.of("taMin5", "24", "taMax5", "30"))));
        server.expect(requestTo(landUri("11B00000", "202607141800")))
            .andRespond(withJson(itemsBody(Map.of("wf5Pm", "흐림"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        assertThat(result.getMinTemperature()).isEqualTo(24);
        assertThat(result.getMaxTemperature()).isEqualTo(30);
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.CLOUDY);
        server.verify();
    }

    @Test
    @DisplayName("date가 tmFc+3일이면 n=3이라 필드가 없어 null을 반환한다")
    void test03() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 18);
        server.expect(requestTo(taUri("11B10101", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of())));
        server.expect(requestTo(landUri("11B00000", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of())));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("n이 3에서 10 범위 밖이면 호출 없이 null을 반환한다")
    void test04() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 15);
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("wf{n}Pm이 없고 wf{n}Am만 있으면 Am을 사용한다")
    void test05() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 19);
        server.expect(requestTo(taUri("11B10101", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("taMin4", "20", "taMax4", "26"))));
        server.expect(requestTo(landUri("11B00000", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("wf4Am", "맑음"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.CLEAR);
        server.verify();
    }

    @Test
    @DisplayName("온도와 wf 둘 다 없으면 null을 반환한다")
    void test06() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 19);
        server.expect(requestTo(taUri("11B10101", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of())));
        server.expect(requestTo(landUri("11B00000", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of())));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("getMidTa는 지점코드를, getMidLandFcst는 규칙으로 계산한 광역 구역코드를 사용한다")
    void test07() {
        var adapter = adapterAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        var date = LocalDate.of(2026, 7, 19);
        server.expect(requestTo(taUri("11B10101", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("taMin4", "24", "taMax4", "30"))));
        server.expect(requestTo(landUri("11B00000", "202607150600")))
            .andRespond(withJson(itemsBody(Map.of("wf4Pm", "맑음"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        server.verify();
    }
    private KmaProperties properties() {
        return new KmaProperties(SERVICE_KEY,2000,2000,new KmaProperties.BaseUrl("https://weather.example.test/VilageFcstInfoService_2.0","https://weather.example.test/MidFcstInfoService","https://weather.example.test/AsosDalyInfoService","https://weather.example.test/LivingWthrIdxServiceV5"));
    }
    private org.springframework.test.web.client.ResponseCreator withJson(String body) {
        return withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body);
    }
    private String itemsBody(Map<String,String> fields) { return itemsBody(fields,true); }
    private String itemsBody(Map<String,String> fields,boolean hasItem) {
        String item=fields.entrySet().stream().map(e -> "\""+e.getKey()+"\":\""+e.getValue()+"\"").collect(Collectors.joining(",","{","}"));
        return "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"items\":{\"item\":"+(hasItem?"["+item+"]":"[]")+"}}}}";
    }
    private KmaMidTermForecastAdapter adapterAt(LocalDateTime now) {
        RestClient.Builder builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();return new KmaMidTermForecastAdapter(new KmaApiClient(builder.build(),SERVICE_KEY),new KmaBaseTime(Clock.fixed(now.atZone(SEOUL).toInstant(),SEOUL)),properties());
    }
    private String taUri(String regId,String tmFc) {return BASE_URL+"/getMidTa?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&regId="+regId+"&tmFc="+tmFc+"&numOfRows=10";}
    private String landUri(String regId,String tmFc) {return BASE_URL+"/getMidLandFcst?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&regId="+regId+"&tmFc="+tmFc+"&numOfRows=10";}

}
