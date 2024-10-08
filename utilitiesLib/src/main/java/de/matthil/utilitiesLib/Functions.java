package de.matthil.utilitiesLib;

public class Functions {
    public static String formatTime(long ms) {
        return String.format("%02d:%02d.%01d0", ms / 60_000, (ms / 1_000) % 60, (ms / 100) % 10);
    }
}
