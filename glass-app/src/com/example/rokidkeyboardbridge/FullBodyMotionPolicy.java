package com.example.rokidkeyboardbridge;

/** Pure timing policy for full-body rendering, independent of portrait animation. */
final class FullBodyMotionPolicy {
    static int idleFrame(long elapsed) {
        long time = Math.max(0, elapsed) % 6000L;
        if (time < 4500) return 0;
        if (time < 4620) return 1;
        if (time < 4700) return 2;
        if (time < 4780) return 3;
        if (time < 4900) return 4;
        return 5;
    }
    static boolean usesIdleFrames(int expression, int mode) {
        return !isSeated(poseForState(expression, mode)) && (expression == 0 || expression == 14);
    }
    static boolean isSeated(int pose) {
        return (pose >= 128 && pose <= 145) || pose == 160 || pose == 161 || (pose >= 165 && pose <= 168);
    }
    static int poseForState(int expression,int mode){
        int pose=Math.max(0,Math.min(167,expression));
        if(mode==1)return 4;
        if(mode==0&&(pose==0||pose==14))return 168;
        return FullBodyExpressionMap.pose(pose);
    }
    // Artwork is normalized to one standing body height, with the same floor baseline.
    static float bodyHeightFraction(int pose) { return isSeated(pose) ? 0.76f : 1f; }
    static float breathScale(long elapsed, int mode) { return 1f; }
    static float speechAngle(long elapsed, int mode) { return 0f; }
}
