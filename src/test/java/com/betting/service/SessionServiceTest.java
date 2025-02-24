package com.betting.service;

import org.junit.Assert;
import org.junit.Test;


/**
 * 会话业务服务测试类
 *
 **/
public class SessionServiceTest {

    private SessionService sessionService = new SessionService();

    @Test
    public void testSessionUniqueness() {
        // 同一用户多次获取会话应相同
        String sessionKey1 = sessionService.getOrCreateSession(1234);
        String sessionKey2 = sessionService.getOrCreateSession(1234);
        Assert.assertEquals("同一用户会话Key必须一致", sessionKey1, sessionKey2);

        // 不同用户会话必须不同
        String sessionKey3 = sessionService.getOrCreateSession(4321);
        Assert.assertNotEquals("不同用户会话Key必须不同", sessionKey1, sessionKey3);
    }

    @Test
    public void testSessionExpiration() throws InterruptedException {
        String sessionKey = sessionService.getOrCreateSession(1234);

        // 模拟过期（原配置10分钟，测试时临时改为1秒）
        Thread.sleep(1500);
        Integer customerId = sessionService.validateSession(sessionKey);
        Assert.assertNull("过期会话必须返回null", customerId == null ? null : String.valueOf(customerId));
    }
}
