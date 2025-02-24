package com.betting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import com.betting.handler.RouterHandler;
import com.betting.util.ThreadPoolManagerUtils;
import com.sun.net.httpserver.HttpServer;

/**
 * 应用服务入口类
 *
 * @author dufy
 **/
public class BettingServerMain {

    /**
     * 服务器监听端口号。 默认为 8001。
     */
    public static final int SERVER_PORT = 8001;

    public static void main(String[] args) throws IOException {

        // 自定义创建线程池
        ThreadPoolExecutor executor = ThreadPoolManagerUtils.createExecutor();

        // 创建 HttpServer 实例，绑定到指定端口
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        // 配置路由
        server.createContext("/", new RouterHandler());

        // 设置线程池执行器
        server.setExecutor(executor);

        // 启动服务器
        server.start();

        // 打印服务器启动信息
        System.out.println("服务启动成功，端口号： " + SERVER_PORT);
    }
}
