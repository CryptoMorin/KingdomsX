package org.kingdoms.server.location;

final class LocationUtils {
    public static int toBlock(double num) {
        int floor = (int) num;
        return (double) floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }
}
