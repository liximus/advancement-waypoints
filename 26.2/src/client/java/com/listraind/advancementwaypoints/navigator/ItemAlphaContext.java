package com.listraind.advancementwaypoints.navigator;

public class ItemAlphaContext {
    private static float currentAlpha = 1.0f;

    public static void setAlpha(float alpha) {
        currentAlpha = alpha;
    }

    public static float getAlpha() {
        return currentAlpha;
    }

    public static void reset() {
        currentAlpha = 1.0f;
    }
}
