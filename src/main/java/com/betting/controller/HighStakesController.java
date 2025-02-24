package com.betting.controller;

import com.betting.enums.HttpStatusEnum;
import com.betting.service.StakeService;
import com.betting.util.HttpHelperUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * 投注项列表控制器
 *
 * @author dufy
 **/
public class HighStakesController {

    private static final StakeService stakeService = new StakeService();

    public static void handle(HttpExchange exchange) throws IOException {

        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                HttpHelperUtils.sendBadRequest(exchange);
                return;
            }
            int betOfferId;
            try {
                betOfferId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.BAD_REQUEST.getCode(), "betOfferId not integer");
                return;
            }

            String result = stakeService.getHighStakes(betOfferId);
            System.out.println("投注项列表结果 :" + result);

            exchange.getResponseHeaders().set("Content-Type", "text/csv");
            HttpHelperUtils.sendSuccess(exchange, result);
        } catch (Exception e) {
            HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.INTERNAL_SERVER_ERROR);
        }
    }
}
