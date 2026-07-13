package com.listraind.advancementwaypoints.advancement;

public class AdvancementTabCapture {
    private static int contentX = 0;
    private static int contentY = 0;

    public static void set(int x, int y) {
        contentX = x;
        contentY = y;
    }

    public static int getX() {
        return contentX;
    }

    public static int getY() {
        return contentY;
    }
}
