package yuuine.xxrag.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketHeartbeatTask {

    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        RagWebSocketHandler.broadcast(
                "{\"type\":\"heartbeat\"}"
        );
    }
}
