package com.superliminal.util.android;

public class MathUtils {

    private static int TABLE_SIZE = 512;
    private static double[] cos_table = new double[TABLE_SIZE];
    private static double[] sin_table = new double[TABLE_SIZE];
    private static double RADIANS_PER_ROW = 2 * Math.PI / TABLE_SIZE;
    static {
        for(int row = 0; row < TABLE_SIZE; row++) {
            cos_table[row] = Math.cos(row * RADIANS_PER_ROW);
            sin_table[row] = Math.sin(row * RADIANS_PER_ROW);
        }
    }

    public final static double DTOR(double deg) {
        return(deg * Math.PI / 180);
    }

    public final static double RTOD(double rad) {
        return rad * 180 / Math.PI;
    }

    /**
     * Fast low-fidelity table-based sine function
     * for when speed is more important than accuracy.
     * 
     * @param deg degrees
     * @return Sine of deg.
     */
    public static double fastSin(double deg) {
        return lookUp(deg, sin_table);
    }

    /**
     * Fast low-fidelity table-based cosine function
     * for when speed is more important than accuracy.
     * 
     * @param deg degrees
     * @return Cosine of deg.
     */
    public static double fastCos(double deg) {
        return lookUp(deg, cos_table);
    }

    private static double lookUp(double deg, double[] from) {
        deg %= 360;
        if(deg < 0)
            deg += 360;
        int row = (int) Math.floor(DTOR(deg) / RADIANS_PER_ROW);
        if(row < 0 || row >= TABLE_SIZE)
            return 0;
        double ret = from[row];
        return ret;
    }

}
