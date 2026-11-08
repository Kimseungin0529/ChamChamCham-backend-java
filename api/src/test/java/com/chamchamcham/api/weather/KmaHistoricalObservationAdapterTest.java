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

class KmaHistoricalObservationAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/AsosDalyInfoService";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private KmaHistoricalObservationAdapter adapter;
    private final WeatherLocation seoulLocation = new WeatherLocation(37.5665,126.9780,"서울 중구 세종대로 110",null);
    private final LocalDate date=LocalDate.of(2026,7,14);

    @Test
    @DisplayName("정상 응답이면 최저최고와 상태를 파싱한다")
    void test01() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson(itemsBody(Map.of("minTa", "25.2", "maxTa", "31.0", "avgTca", "8.9", "sumRn", "74.5"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        assertThat(result.getMinTemperature()).isEqualTo(25);
        assertThat(result.getMaxTemperature()).isEqualTo(31);
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.RAIN);
        server.verify();
    }

    @Test
    @DisplayName("sumRn이 빈 문자열이면 무강수로 판정해 전운량으로 상태를 정한다")
    void test02() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson(itemsBody(Map.of("minTa", "20.0", "maxTa", "26.0", "avgTca", "8.9", "sumRn", ""))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNotNull();
        assertThat(result.getCondition()).isEqualTo(WeatherCondition.CLOUDY);
        server.verify();
    }

    @Test
    @DisplayName("minTa가 없으면 null을 반환한다")
    void test03() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson(itemsBody(Map.of("maxTa", "31.0", "avgTca", "8.9", "sumRn", "0"))));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("items가 빈 리스트면(오늘 조회 99) null을 반환한다")
    void test04() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson(itemsBody(Map.of(), false)));
        var result = adapter.fetch(seoulLocation, date);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("요청 파라미터는 startDt와 endDt가 같고 최근접 stnIds를 사용한다")
    void test05() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson(itemsBody(Map.of("minTa", "25.2", "maxTa", "31.0", "avgTca", "8.9", "sumRn", ""))));
        adapter.fetch(seoulLocation, date);
        server.verify();
    }

    @Test
    @DisplayName("첫 ASOS item이 null이면 데이터 없음으로 반환한다")
    void nullFirstItemReturnsNoData() {
        server.expect(requestTo(requestUri()))
            .andRespond(withJson("""
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[null]}}}}
                """));

        assertThat(adapter.fetch(seoulLocation, date)).isNull();
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
    @BeforeEach void setUp() {
        RestClient.Builder builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();adapter=new KmaHistoricalObservationAdapter(new KmaApiClient(builder.build(),SERVICE_KEY),properties());
    }
    private String requestUri() {return BASE_URL+"/getWthrDataList?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&dataCd=ASOS&dateCd=DAY&startDt=20260714&endDt=20260714&stnIds=108&numOfRows=10";}

}
