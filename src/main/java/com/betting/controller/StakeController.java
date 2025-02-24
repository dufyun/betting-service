package com.betting.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.betting.enums.HttpStatusEnum;
import com.betting.service.SessionService;
import com.betting.service.StakeService;
import com.betting.util.HttpHelperUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * 提交投注控制器
 *
 * @author dufy
 **/
public class StakeController {

    private static final SessionService sessionService = new SessionService();
    private static final StakeService stakeService = new StakeService();

    public static void handle(HttpExchange exchange) throws IOException {
        try {
            // 解析请求路径和参数
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                HttpHelperUtils.sendBadRequest(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String sessionKey = HttpHelperUtils.getSessionKey(query);
            // 验证会话有效性
            Integer customerId = sessionService.validateSession(sessionKey);
            if (customerId == null) {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.UNAUTHORIZED);
                return;
            }

            // 解析并更新用户投注项的投注金额
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            String body = reader.readLine();
            int stake;
            try {
                stake = Integer.parseInt(body);
            } catch (NumberFormatException e) {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.BAD_REQUEST.getCode(), "stake not integer");
                return;
            }
            int betOfferId;
            try {
                betOfferId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.BAD_REQUEST.getCode(), "betOfferId not integer");
                return;
            }
            stakeService.submitStake(betOfferId, customerId, stake);
            HttpHelperUtils.sendSuccess(exchange, "");
        }catch (Exception e) {
            HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.INTERNAL_SERVER_ERROR);
        }
    }
}
