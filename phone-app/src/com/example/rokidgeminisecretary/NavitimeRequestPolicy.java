package com.example.rokidgeminisecretary;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Pure request construction; credentials and network access belong to the client. */
final class NavitimeRequestPolicy {
    private static final DateTimeFormatter API_TIME =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss", Locale.ROOT);
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private NavitimeRequestPolicy() {}

    static String normalizeDeparture(String departure) {
        if (departure == null || !departure.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}(:[0-9]{2})?")) {
            throw new IllegalArgumentException("navitime_invalid_departure");
        }
        try {
            // LocalDateTime.toString() omits zero seconds; the API requires HH:mm:ss.
            return LocalDateTime.parse(departure).format(API_TIME);
        } catch (DateTimeParseException invalid) {
            throw new IllegalArgumentException("navitime_invalid_departure");
        }
    }

    static String departureTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(TOKYO).format(API_TIME);
    }

    // https://api-sdk.navitime.co.jp/api/specs/api_guide/route_transit.html
    static String routeQuery(double lat, double lng, double toLat, double toLng, String departure) {
        return commonQuery(lat, lng, toLat, toLng, departure) + "&shape=true&limit=1";
    }
    static String choicesQuery(double lat,double lng,double x,double y,String departure) {
        return commonQuery(lat,lng,x,y,departure)+"&shape=true&limit=3";
    }

    // transport_shape is a shape_transit option, not a route_transit option.
    // no identifies the selected route's summary.no; omitting it selects the first route.
    // https://api-sdk.navitime.co.jp/api/specs/api_guide/shape_transit.html
    static String shapeQuery(double lat, double lng, double toLat, double toLng,
                             String departure, String routeNumber) {
        String query = commonQuery(lat, lng, toLat, toLng, departure)
            + "&format=geojson&options=transport_shape&limit=1";
        if (routeNumber != null && !routeNumber.isEmpty()) {
            if (!routeNumber.matches("[0-9]+")) {
                throw new IllegalArgumentException("navitime_invalid_route_number");
            }
            try {
                Integer.parseInt(routeNumber);
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("navitime_invalid_route_number");
            }
            query += "&no=" + encode(routeNumber);
        }
        return query;
    }

    private static String commonQuery(double lat, double lng, double toLat, double toLng,
                                      String departure) {
        validateCoordinate(lat, lng);
        validateCoordinate(toLat, toLng);
        return "start=" + encode(lat + "," + lng)
            + "&goal=" + encode(toLat + "," + toLng)
            + "&start_time=" + encode(normalizeDeparture(departure))
            + "&coord_unit=degree&datum=wgs84";
    }

    private static void validateCoordinate(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)
            || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw new IllegalArgumentException("navitime_invalid_coordinate");
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException unavailable) {
            throw new IllegalStateException("navitime_encoding_unavailable");
        }
    }
}
