package com.althico.p2pservice.config;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SignalingWebSocketHandler extends TextWebSocketHandler {

    // 房间号 -> 房间内所有连接
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = getRoomIdFromQuery(session);
        if (roomId == null) roomId = "default";

        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        session.getAttributes().put("roomId", roomId);

        System.out.println("用户加入房间: " + roomId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId == null) return;

        Set<WebSocketSession> sessions = rooms.getOrDefault(roomId, Set.of());
        for (WebSocketSession s : sessions) {
            // 发送给房间内除了自己以外的其他人
            if (!s.getId().equals(session.getId()) && s.isOpen()) {
                s.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null) {
            Set<WebSocketSession> room = rooms.get(roomId);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
        System.out.println("用户离开房间: " + roomId);
    }

    // 从 ?roomId=xxx 参数中解析房间号
    private String getRoomIdFromQuery(WebSocketSession session) {
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("roomId=")) {
                    return param.split("=")[1];
                }
            }
        }
        return null;
    }
}
