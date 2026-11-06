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

class KmaApiClientTest {
    private MockRestServiceServer server;
    private static final String SERVICE_KEY = "test-service-key";
    private static final String BASE_URL = "https://weather.example.test/VilageFcstInfoService_2.0";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private KmaApiClient client;
    private static final Map<String,String> PARAMS = new LinkedHashMap<>();
    static { PARAMS.put("numOfRows","10");PARAMS.put("nx","60");PARAMS.put("ny","127"); }

    @Test
    @DisplayName("resultCode 00이면 item 목록을 그대로 반환한다")
    void test01() {
        server.expect(requestTo(requestUri("getUltraSrtNcst")))
            .andRespond(withJson(SUCCESS_BODY));
        var items = client.getItems(BASE_URL, "getUltraSrtNcst", PARAMS, TestItem.class);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCategory()).isEqualTo("T1H");
        assertThat(items.get(0).getObsrValue()).isEqualTo("24.4");
        server.verify();
    }

    @Test
    @DisplayName("resultCode 03이면 예외 없이 빈 리스트를 반환한다")
    void test02() {
        server.expect(requestTo(requestUri("getVilageFcst")))
            .andRespond(withJson(bodyOf("03", "NODATA_ERROR")));
        var items = client.getItems(BASE_URL, "getVilageFcst", PARAMS, TestItem.class);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("resultCode 99이면 예외 없이 빈 리스트를 반환한다")
    void test03() {
        server.expect(requestTo(requestUri("getWthrDataList")))
            .andRespond(withJson(bodyOf("99", "전날 자료까지 제공됩니다")));
        var items = client.getItems(BASE_URL, "getWthrDataList", PARAMS, TestItem.class);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("resultCode 30이면 제공자 불가 예외를 던진다")
    void test04() {
        server.expect(requestTo(requestUri("getMidTa")))
            .andRespond(withJson(bodyOf("30", "등록되지 않은 서비스키입니다")));
        var exception = assertThrows(BusinessException.class, () -> client.getItems(BASE_URL, "getMidTa", PARAMS, TestItem.class));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("HTTP 401이면 제공자 불가 예외를 던진다")
    void test05() {
        server.expect(requestTo(requestUri("getUVIdxV5")))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        var exception = assertThrows(BusinessException.class, () -> client.getItems(BASE_URL, "getUVIdxV5", PARAMS, TestItem.class));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("item이 배열이 아니라 단일 객체로 와도 한 건짜리 리스트로 흡수한다")
    void test06() {
        server.expect(requestTo(requestUri("getWthrDataList")))
            .andRespond(withJson(SINGLE_ITEM_BODY));
        var items = client.getItems(BASE_URL, "getWthrDataList", PARAMS, TestItem.class);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCategory()).isEqualTo("T1H");
    }

    @Test
    @DisplayName("JSON을 요청해도 XML 오류 응답이 오면 제공자 불가 예외를 던진다")
    void test07() {
        server.expect(requestTo(requestUri("getVilageFcst")))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.TEXT_XML)
                    .body("<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg></cmmMsgHeader></OpenAPI_ServiceResponse>")
            );
        var exception = assertThrows(BusinessException.class, () -> client.getItems(BASE_URL, "getVilageFcst", PARAMS, TestItem.class));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }
    @BeforeEach void setUp() {
        RestClient.Builder builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        client=new KmaApiClient(builder.build(),SERVICE_KEY);
    }
    private String requestUri(String operation) { return BASE_URL+"/"+operation+"?serviceKey="+SERVICE_KEY+"&dataType=JSON&pageNo=1&numOfRows=10&nx=60&ny=127"; }
    private String bodyOf(String code,String msg) { return "{\"response\":{\"header\":{\"resultCode\":\""+code+"\",\"resultMsg\":\""+msg+"\"},\"body\":{\"items\":{\"item\":[]}}}}"; }
    private record TestItem(@JsonProperty("category") String category,@JsonProperty("obsrValue") String obsrValue) {
        public String getCategory() { return category; }
        public String getObsrValue() { return obsrValue; }
    }
    private org.springframework.test.web.client.ResponseCreator withJson(String body) {
        return withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body);
    }
    private String emptyBodyOf(String code) {
        return "{\"response\":{\"header\":{\"resultCode\":\""+code+"\",\"resultMsg\":\"NODATA_ERROR\"},\"body\":{\"items\":{\"item\":[]}}}}";
    }
    private String unusedBodyOf(String... items) {
        return "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},\"body\":{\"items\":{\"item\":["+String.join(",",items)+"]}}}}";
    }
    private static final String SUCCESS_BODY = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"dataType":"JSON","items":{"item":[
               {"category":"T1H","obsrValue":"24.4"}
             ]},"pageNo":1,"numOfRows":10,"totalCount":1}}}
        """;
    private static final String SINGLE_ITEM_BODY = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
             "body":{"dataType":"JSON","items":{"item":
               {"category":"T1H","obsrValue":"25.2"}
             },"pageNo":1,"numOfRows":10,"totalCount":1}}}
        """;

}
