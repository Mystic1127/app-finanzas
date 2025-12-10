package com.example.finanzas.util;

public final class PinSession {
    private static boolean unlocked = false;

    private PinSession() {
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static void unlock() {
        unlocked = true;
    }

    public static void lock() {
        unlocked = false;
    }
}
