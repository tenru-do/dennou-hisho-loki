package com.example.rokidgeminisecretary;
public final class GooglePolylineTest {
    public static void main(String[] args) {
        double[][] points = GooglePolyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        double[][] expected = {{38.5,-120.2},{40.7,-120.95},{43.252,-126.453}};
        if (points.length != 3) throw new AssertionError();
        for (int i=0;i<3;i++) for(int j=0;j<2;j++)
            if (Math.abs(points[i][j]-expected[i][j])>0.000001) throw new AssertionError();
        for (String invalid : new String[]{"", "_", "??", "~~~~~~~~~~~", "\n???", "_p~iF~ps|U_"}) {
            try { GooglePolyline.decode(invalid); throw new AssertionError("accepted invalid polyline"); }
            catch (IllegalArgumentException expectedFailure) { }
        }
        double[][] zero = GooglePolyline.decode("????");
        if(zero.length!=2 || zero[1][0]!=0) throw new AssertionError();
        System.out.println("Google polyline: coordinates, signed deltas, truncation and invalid input PASS");
    }
}
