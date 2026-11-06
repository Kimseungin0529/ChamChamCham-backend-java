package com.chamchamcham.api.weather;

import java.util.Comparator;
import java.util.List;
import static java.lang.Math.*;

/** Haversine 거리의 최소 지점을 고르며, 동률은 목록의 첫 지점을 유지한다. */
public final class NearestMidFcstStationResolver {
    public static final NearestMidFcstStationResolver INSTANCE = new NearestMidFcstStationResolver();
    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double DEGRAD = PI / 180.0;
    private NearestMidFcstStationResolver() {}
    public static MidFcstStation resolve(double latitude, double longitude) {
        return resolve(latitude, longitude, MidFcstStations.ALL);
    }
    public static MidFcstStation resolve(double latitude, double longitude, List<MidFcstStation> stations) {
        return stations.stream().min(Comparator.comparingDouble(station -> haversineDistanceKm(latitude,longitude,station.getLatitude(),station.getLongitude()))).orElseThrow();
    }
    private static double haversineDistanceKm(double lat1,double lon1,double lat2,double lon2) {
        double dLat=(lat2-lat1)*DEGRAD, dLon=(lon2-lon1)*DEGRAD;
        double sinHalfDLat=sin(dLat/2), sinHalfDLon=sin(dLon/2);
        double a=sinHalfDLat*sinHalfDLat+cos(lat1*DEGRAD)*cos(lat2*DEGRAD)*sinHalfDLon*sinHalfDLon;
        return 2*EARTH_RADIUS_KM*asin(sqrt(a));
    }
}
