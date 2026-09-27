package com.example.rokidgeminisecretary;

/** Only zero routes permits fallback; authentication, quota and network errors must stay visible. */
final class WalkingFallback {
    interface Request<T> { T fetch(String mode) throws Exception; }
    static final class Result<T> {
        final T value;
        final String mode;
        Result(T value, String mode) { this.value = value; this.mode = mode; }
    }
    static <T> Result<T> fetch(String requested, Request<T> request) throws Exception {
        try { return new Result<T>(request.fetch(requested), requested); }
        catch (Exception error) {
            if (!"bicycling".equals(requested) || !"google_route_not_found".equals(error.getMessage())) throw error;
            return new Result<T>(request.fetch("walking"), "walking");
        }
    }
}
