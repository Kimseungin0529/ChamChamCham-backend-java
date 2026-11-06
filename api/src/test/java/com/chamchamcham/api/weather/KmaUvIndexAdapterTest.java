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

class KmaUvIndexAdapterTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/LivingWthrIdxServiceV5";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("pnu 앞 10자리(법정동코드)가 아니라 앞 5자리+00000(시군구코드)으로 요청한다")
    void test01() {
        var target = adapter();
        var location = locationWithPnu("1114010300xxxxxxxxx");
        server.expect(requestTo(uvUri("1114000000", "2026071509")))
            .andRespond(withJson(itemsBody(Map.of("h0", "1"))));
        var result = target.fetch(location);
        assertThat(result).isEqualTo(1);
        server.verify();
    }

    @Test
    @DisplayName("h0을 정수로 파싱한다")
    void test02() {
        var target = adapter();
        var location = locationWithPnu("1114010300xxxxxxxxx");
        server.expect(requestTo(uvUri("1114000000", "2026071509")))
            .andRespond(withJson(itemsBody(Map.of("h0", "5"))));
        assertThat(target.fetch(location)).isEqualTo(5);
    }

    @Test
    @DisplayName("pnu가 null이면 호출 없이 null을 반환한다")
    void test03() {
        var target = adapter();
        var location = locationWithPnu(null);
        var result = target.fetch(location);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("pnu가 5자리 미만이면 null을 반환한다")
    void test04() {
        var target = adapter();
        var location = locationWithPnu("1114");
        var result = target.fetch(location);
        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("items가 빈 리스트면 null을 반환한다")
    void test05() {
        var target = adapter();
        var location = locationWithPnu("1114010300xxxxxxxxx");
        server.expect(requestTo(uvUri("1114000000", "2026071509")))
            .andRespond(withJson(itemsBody(Map.of(), false)));
        var result = target.fetch(location);
        assertThat(result).isNull();
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
    private KmaUvIndexAdapter adapter() {
        RestClient.Builder builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();return new KmaUvIndexAdapter(new KmaApiClient(builder.build(),SERVICE_KEY),new KmaBaseTime(Clock.fixed(LocalDateTime.of(2026,7,15,10,0).atZone(SEOUL).toInstant(),SEOUL)),properties());
    }
    private WeatherLocation locationWithPnu(String pnu) {return new WeatherLocation(37.5665,126.9780,"서울시 중구 세종대로 110",pnu);}
    private String uvUri(String areaNo,String time) {return BASE_URL+"/getUVIdxV5?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&areaNo="+areaNo+"&time="+time+"&numOfRows=10";}

}
