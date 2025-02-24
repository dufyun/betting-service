package com.betting.util;

import com.betting.enums.HttpStatusEnum;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 处理工具类
 *
 * @author dufy
 */
public final class HttpHelperUtils {
    /**
     * 查询参数中的 sessionkey 键
     */
    private static final String SESSION_KEY = "sessionkey";

    /**
     * 发送一个成功的响应，不带具体内容
     *
     * @param exchange HttpExchange 对象，用于获取请求和发送响应
     * @throws IOException 如果发送响应时出现异常
     */
    public static void sendSuccess(HttpExchange exchange) throws IOException {
        sendResponse(exchange, HttpStatusEnum.SUCCESS);
    }

    /**
     * 发送一个成功的响应，带有具体的响应内容
     *
     * @param exchange HttpExchange 对象，用于获取请求和发送响应
     * @param body 响应的内容体
     * @throws IOException 如果发送响应时出现异常
     */
    public static void sendSuccess(HttpExchange exchange, String body) throws IOException {
        sendResponse(exchange, HttpStatusEnum.SUCCESS.getCode(), body);
    }

    /**
     * 发送一个错误请求的响应
     *
     * @param exchange HttpExchange 对象，用于获取请求和发送响应
     * @throws IOException 如果发送响应时出现异常
     */
    public static void sendBadRequest(HttpExchange exchange) throws IOException {
        sendResponse(exchange, HttpStatusEnum.BAD_REQUEST);
    }

    /**
     * 发送一个 HTTP 响应，使用 HttpStatusEnum 中的状态码和消息
     *
     * @param exchange HttpExchange 对象，用于获取请求和发送响应
     * @param httpStatusEnum HTTP 状态枚举，包含状态码和消息
     * @throws IOException 如果发送响应时出现异常
     */
    public static void sendResponse(HttpExchange exchange, HttpStatusEnum httpStatusEnum) throws IOException {
        String body = httpStatusEnum.getMessage();
        if (body == null) {
            body = "";
        }
        exchange.sendResponseHeaders(httpStatusEnum.getCode(), body.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }

    /**
     * 发送一个自定义的 HTTP 响应，指定状态码和内容
     *
     * @param exchange HttpExchange 对象，用于获取请求和发送响应
     * @param code 响应的 HTTP 状态码
     * @param body 响应的内容体
     * @throws IOException 如果发送响应时出现异常
     */
    public static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        if (body == null) {
            body = "";
        }
        exchange.sendResponseHeaders(code, body.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }

    /**
     * 解析查询字符串，将其转换为键值对映射
     *
     * @param query 查询字符串，格式如 "key1=value1&key2=value2"
     * @return 键值对映射，键为查询参数名，值为查询参数值
     */
    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) {
            return new HashMap<>();
        }

        return Arrays.stream(query.split("&")).map(p -> p.split("=", 2))
            .collect(Collectors.toMap(arr -> arr[0], arr -> arr.length > 1 ? arr[1] : ""));
    }

    /**
     * 从查询字符串中获取 sessionkey 参数的值
     *
     * @param query 查询字符串
     * @return sessionkey 参数的值，若不存在则返回 null
     */
    public static String getSessionKey(String query) {
        Map<String, String> params = parseQuery(query);
        return params.get(SESSION_KEY);
    }
}