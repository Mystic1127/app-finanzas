package com.example.finanzas.util;

import android.os.SystemClock;
import android.util.Log;

import com.example.finanzas.BuildConfig;

public final class PerfLogger {
    private static final String PREFIX = "SpendlyPerf/";

    private PerfLogger() { }

    public static long now() {
        return SystemClock.elapsedRealtime();
    }

    public static void log(String screen, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(PREFIX + screen, message);
        }
    }

    public static void logSince(String screen, String event, long startMs) {
        if (BuildConfig.DEBUG && startMs > 0L) {
            log(screen, event + "=" + (now() - startMs) + "ms");
        }
    }
}
