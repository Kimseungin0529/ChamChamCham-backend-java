package com.chamchamcham.api.weather;

import static java.lang.Math.*;

/** 기상청 LCC 표준 파라미터로 위경도를 5km 격자에 투영한다. */
public final class GeoToGridConverter {
    public static final GeoToGridConverter INSTANCE = new GeoToGridConverter();
    private static final double RE=6371.00877, GRID=5.0, SLAT1=30.0, SLAT2=60.0, OLON=126.0, OLAT=38.0;
    private static final int XO=43, YO=136;
    private static final double DEGRAD=PI/180.0;
    private GeoToGridConverter() {}
    public static KmaGrid convert(double latitude,double longitude) {
        double re=RE/GRID, slat1=SLAT1*DEGRAD, slat2=SLAT2*DEGRAD, olon=OLON*DEGRAD, olat=OLAT*DEGRAD;
        double sn=tan(PI*0.25+slat2*0.5)/tan(PI*0.25+slat1*0.5);
        sn=log(cos(slat1)/cos(slat2))/log(sn);
        double sf=tan(PI*0.25+slat1*0.5);
        sf=pow(sf,sn)*cos(slat1)/sn;
        double ro=tan(PI*0.25+olat*0.5);
        ro=re*sf/pow(ro,sn);
        double ra=tan(PI*0.25+latitude*DEGRAD*0.5);
        ra=re*sf/pow(ra,sn);
        double theta=longitude*DEGRAD-olon;
        if (theta>PI) theta-=2.0*PI;
        if (theta<-PI) theta+=2.0*PI;
        theta*=sn;
        return new KmaGrid((int)(ra*sin(theta)+XO+0.5),(int)(ro-ra*cos(theta)+YO+0.5));
    }
}
