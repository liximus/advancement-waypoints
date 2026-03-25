package com.listraind.advancementwaypoints;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalCache {
    public static final List<AdvancementWaypointsClient.ParsedAdvancement> CACHED = new CopyOnWriteArrayList<>();
}