package com.looksee.browsing;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;

public class RateLimitExecutor extends HttpCommandExecutor {

    public static final int CONCURRENT_SESSIONS = 1;
    public static final int ACTIONS_RATE_LIMIT_PER_SECOND = 50;

    public static final double SECONDS_PER_ACTION = ((double) CONCURRENT_SESSIONS)
            / ((double) ACTIONS_RATE_LIMIT_PER_SECOND);
    private long lastExecutionTime;

    public RateLimitExecutor(URL addressOfRemoteServer) {
        super(addressOfRemoteServer);
        lastExecutionTime = 0;
    }

    public Response execute(Command command) throws IOException {
        long currentTime = Instant.now().toEpochMilli();
        double elapsedTime = TimeUnit.MILLISECONDS.toSeconds(currentTime - lastExecutionTime);
        if (elapsedTime < SECONDS_PER_ACTION) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis((long)(SECONDS_PER_ACTION - elapsedTime)));
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
        lastExecutionTime = Instant.now().toEpochMilli();
        return super.execute(command);
    }
}