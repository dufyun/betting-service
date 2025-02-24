package com.betting.handler;

import java.io.IOException;
import java.util.regex.Pattern;

import com.betting.controller.HighStakesController;
import com.betting.controller.SessionController;
import com.betting.controller.StakeController;
import com.betting.enums.HttpStatusEnum;
import com.betting.util.HttpHelperUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 统一路由请求处理器
 *
 * @author dufy
 */
public class RouterHandler implements HttpHandler {
    private static final Pattern SESSION_PATTERN = Pattern.compile("/\\d+/session");
    private static final Pattern STAKE_PATTERN = Pattern.compile("/\\d+/stake");
    private static final Pattern HIGH_STAKES_PATTERN = Pattern.compile("/\\d+/highstakes");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (SESSION_PATTERN.matcher(path).matches()) {
                SessionController.handle(exchange);
            } else if (STAKE_PATTERN.matcher(path).matches()) {
                if (!"POST".equalsIgnoreCase(method)) {
                    HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.METHOD_NOT_ALLOWED);
                    return;
                }
                StakeController.handle(exchange);
            } else if (HIGH_STAKES_PATTERN.matcher(path).matches()) {
                HighStakesController.handle(exchange);
            } else {
                HttpHelperUtils.sendResponse(exchange, HttpStatusEnum.NOT_FOUND);
            }
        } finally {
            exchange.close();
        }
    }

}
