package ${packagePath};
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
@ServerEndpoint("/ws/{userId}")
@Component
public class WebSocketEndpoint {
    @OnOpen
    public void onOpen(Session session) { /* 建立连接 */ }
    @OnMessage
    public void onMessage(String message, Session session) { /* 接收消息 */ }
    @OnClose
    public void onClose(Session session) { /* 关闭连接 */ }
    @OnError
    public void onError(Session session, Throwable error) { /* 错误处理 */ }
}
