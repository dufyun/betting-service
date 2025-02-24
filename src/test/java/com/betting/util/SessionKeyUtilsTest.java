package com.betting.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Function: 会话密钥生成器测试类
 *
 * @author dufy
 **/
public class SessionKeyUtilsTest {

    private static final int CONCURRENCY_LEVEL = 1000;

    @Test
    public void testKeyUniquenessUnderConcurrency() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY_LEVEL);
        Set<String> keys = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // 并发生成密钥
        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            pool.submit(() -> {
                String key = SessionKeyUtils.generate();
                Assert.assertTrue("检测到重复Key: " + key, keys.add(key));
                Assert.assertTrue("密钥格式不符: " + key, key.matches("^[2-9A-Za-z]{8}$"));
            });
        }

        pool.shutdown();
        Assert.assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertEquals(CONCURRENCY_LEVEL, keys.size());
    }
}
