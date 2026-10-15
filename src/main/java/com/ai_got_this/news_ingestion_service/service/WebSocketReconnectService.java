package com.ai_got_this.news_ingestion_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WebSocketReconnectService {
    private static final long SESSION_CLOSE_TIMEOUT = TimeUnit.SECONDS.toNanos(30);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toNanos(5);

    private final ObjectProvider<WebSocketConnectionManager> connectionManagerProvider;
    private final ScheduledExecutorService reconnectExecutor;

    private int consecutiveAttempts = 0;

    private enum State{
        DISCONNECTING,
        DISCONNECTED,
        WAITING_RETRY,
        CONNECTING,
        CONNECTED,
        TERMINATED
    }

    private State state = State.DISCONNECTED;


    public WebSocketReconnectService(ObjectProvider<WebSocketConnectionManager> connectionManagerProvider,
                                    @Qualifier("alpacaReconnectExecutor") ScheduledExecutorService reconnectExecutor) {
        this.connectionManagerProvider = connectionManagerProvider;
        this.reconnectExecutor = reconnectExecutor;
    }

    private void openConnectionOnExecutorThread(WebSocketConnectionManager connectionManager){
        try{
            connectionManager.stop();
            connectionManager.start();
            reconnectExecutor.schedule(() -> validateConnectionOnExecutorThread(connectionManager),
                    CONNECTION_TIMEOUT, TimeUnit.NANOSECONDS);
        }
        catch(Exception e){
            log.error("An error occurred while opening WebSocket connection: {}", e.getMessage(), e);
            if (consecutiveAttempts > 5){
                log.warn("Too many consecutive connection attempts. No further retries.");
                state = State.TERMINATED;
                return;
            }
            state = State.DISCONNECTED;
            scheduleReconnectOnExecutorThread();
        }
    }

    public void disconnect(WebSocketSession session){
        reconnectExecutor.execute(() -> disconnectOnExecutorThread(session));
    }

    private void disconnectOnExecutorThread(WebSocketSession session){
        if (state != State.CONNECTED){
            return;
        }

        state = State.DISCONNECTING;
        long waitStartedAt = System.nanoTime();

        while (session.isOpen()) {
            long elapsedNanos = System.nanoTime() - waitStartedAt;
            if (elapsedNanos >= SESSION_CLOSE_TIMEOUT) {
                log.warn("Timed out waiting 30 seconds for the WebSocket session to close. Restarting the application.");
            }
        }

        if (consecutiveAttempts > 5){
            log.warn("Too many consecutive connection attempts. No further retries.");
            state = State.TERMINATED;
            return;
        }
        scheduleReconnectOnExecutorThread();
    }

    private void scheduleReconnectOnExecutorThread(){
        long backoffDelayMillis = calculateBackoffDelay(consecutiveAttempts++);
        reconnectExecutor.schedule(this::connectOnExecutorThread, backoffDelayMillis, TimeUnit.MILLISECONDS);
        state = State.WAITING_RETRY;
    }

    private long calculateBackoffDelay(int connectionTrialCount) {
        int cappedTrialCount = Math.min(Math.max(connectionTrialCount, 0), 4);
        long delaySeconds = Math.min(5L * (1L << cappedTrialCount), 60L);
        return TimeUnit.SECONDS.toMillis(delaySeconds);
    }

    private void connectOnExecutorThread() {
        if (state != State.DISCONNECTED && state != State.WAITING_RETRY) {
            return;
        }

        state = State.CONNECTING;
        WebSocketConnectionManager connectionManager = connectionManagerProvider.getIfAvailable();

        if (connectionManager == null) {
            log.error("Alpaca WebSocket connection manager is not available");
            log.warn("Connection is being terminated");
            state = State.TERMINATED;
            return;
        }

        openConnectionOnExecutorThread(connectionManager);
    }

    private void validateConnectionOnExecutorThread(WebSocketConnectionManager connectionManager) {
        if (connectionManager.isConnected()){
            return;
        }
        log.warn("Connection to Alpaca WebSocket server failed.");
        if (consecutiveAttempts > 5){
            log.warn("Too many consecutive connection attempts. No further retries.");
            state = State.TERMINATED;
            return;
        }
        scheduleReconnectOnExecutorThread();
    }

    public void onSuccessfulConnection(){
        reconnectExecutor.execute(this::onSuccessfulConnectionOnExecutorThread);
    }

    private void onSuccessfulConnectionOnExecutorThread(){
        consecutiveAttempts = 0;
        state = State.CONNECTED;
    }
}
