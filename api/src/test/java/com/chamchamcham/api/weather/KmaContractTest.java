package com.chamchamcham.api.weather;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.weather.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KmaContractTest {
    @Test void propertiesDefaultsAndExplicitTimeoutsBind() {
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("kma.service-key","test-service-key");
        for (String service : List.of("vilage-fcst","mid-fcst","asos","uv")) values.put("kma.base-url."+service,"https://weather.example.test/"+service);
        KmaProperties defaults = bind(values);
        assertThat(defaults.getConnectTimeoutMillis()).isEqualTo(2000);
        assertThat(defaults.getReadTimeoutMillis()).isEqualTo(2000);
        assertThat(defaults.getBaseUrl().getUv()).isEqualTo("https://weather.example.test/uv");
        values.put("kma.connect-timeout-millis","123");
        values.put("kma.read-timeout-millis","456");
        KmaProperties explicit = bind(values);
        assertThat(explicit.getConnectTimeoutMillis()).isEqualTo(123);
        assertThat(explicit.getReadTimeoutMillis()).isEqualTo(456);
    }
    private KmaProperties bind(Map<String,Object> values) {
        return new Binder(new MapConfigurationPropertySource(values)).bind("kma",Bindable.of(KmaProperties.class)).get();
    }
    @Test void missingItemDefaultsEmptyButExplicitNullIsProviderFailure() {
        assertThat(read("{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{}}}}", "test-service-key")).isEmpty();
        assertThatThrownBy(() -> read("{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":null}}}}", "test-service-key"))
            .isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE));
    }
    @Test void encodedKeyAndNullableItemFieldsSurviveProviderParsing() {
        List<Item> result=read("{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[{}, {\"value\":null}]}}}}","test%2Bkey%3D");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).value()).isNull();
        assertThat(result.get(1).value()).isNull();
    }
    private List<Item> read(String body,String key) {
        RestClient.Builder builder=RestClient.builder();
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://weather.example.test/get?serviceKey="+key+"&dataType=JSON&pageNo=1"))
            .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        List<Item> items=new KmaApiClient(builder.build(),key).getItems("https://weather.example.test","get",Map.of(),Item.class);
        server.verify();
        return items;
    }
    private record Item(String value) {}
    @Test void nearestStationsKeepFirstTieAndFailOnEmptyInput() {
        AsosStation a=new AsosStation("a","first",37,127), b=new AsosStation("b","second",37,127);
        assertThat(NearestAsosStationResolver.INSTANCE.resolve(37,127,Arrays.asList(a,b))).isSameAs(a);
        assertThatThrownBy(() -> NearestAsosStationResolver.INSTANCE.resolve(37,127,List.of())).isInstanceOf(NoSuchElementException.class);
        MidFcstStation c=new MidFcstStation("c","first",37,127), d=new MidFcstStation("d","second",37,127);
        assertThat(NearestMidFcstStationResolver.INSTANCE.resolve(37,127,Arrays.asList(c,d))).isSameAs(c);
        assertThatThrownBy(() -> NearestMidFcstStationResolver.INSTANCE.resolve(37,127,List.of())).isInstanceOf(NoSuchElementException.class);
    }
}
