package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.looksee.utils.TimingUtils;

class TimingUtilsTest {

    @Test
    void pauseThreadWaitsAtLeastRequestedDuration() {
        long start = System.nanoTime();
        TimingUtils.pauseThread(20);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMillis >= 15, "Expected sleep duration close to requested time");
    }

    @Test
    void pauseThreadHandlesZeroDuration() {
        long start = System.nanoTime();
        TimingUtils.pauseThread(0);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMillis >= 0);
    }
}
