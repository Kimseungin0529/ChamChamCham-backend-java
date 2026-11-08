package com.chamchamcham.api.weather;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix="kma")
public record KmaProperties(String serviceKey, @DefaultValue("2000") int connectTimeoutMillis,
                            @DefaultValue("2000") int readTimeoutMillis, BaseUrl baseUrl) {
    public KmaProperties {
        Objects.requireNonNull(serviceKey,"serviceKey");
        Objects.requireNonNull(baseUrl,"baseUrl");
    }
    public String getServiceKey() { return serviceKey; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public BaseUrl getBaseUrl() { return baseUrl; }
    public record BaseUrl(String vilageFcst,String midFcst,String asos,String uv) {
        public BaseUrl {
            Objects.requireNonNull(vilageFcst,"vilageFcst");
            Objects.requireNonNull(midFcst,"midFcst");
            Objects.requireNonNull(asos,"asos");
            Objects.requireNonNull(uv,"uv");
        }
        public String getVilageFcst() { return vilageFcst; }
        public String getMidFcst() { return midFcst; }
        public String getAsos() { return asos; }
        public String getUv() { return uv; }
    }
}
