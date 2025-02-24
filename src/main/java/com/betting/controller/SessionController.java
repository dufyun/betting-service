package com.betting.controller;

import java.io.IOException;

import com.betting.enums.HttpStatusEnum;
import com.betting.service.SessionService;
import com.betting.util.HttpHelperUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * 会话控制器
 * 
 * @author dufy
 */
public class SessionController {
    private static final SessionService sessionService = new SessionService();

    public static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                HttpHelperUtils.sendBadRequest(exchange);
                return;
            }
            int customerId;
            try {
                customerId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.BAD_REQUEST.getCode(), "customerId not integer");
                return;
            }

            String sessionKey = sessionService.getOrCreateSession(customerId);
            System.out.println(String.format("用户：%s,对应的sessionKey：%s", customerId, sessionKey));
            HttpHelperUtils.sendSuccess(exchange, sessionKey);

        } catch (Exception e) {
            HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.INTERNAL_SERVER_ERROR);
        }
    }
}