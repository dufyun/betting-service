package com.betting.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * 投注业务服务测试类
 *
 * @author dufy
 **/
public class StakeServiceTest {

    private static final int BET_OFFER = 888;
    private static final int THREAD_COUNT = 100;
    private final StakeService stakeService = new StakeService();

    @Test
    public void testConcurrentStakeSubmission() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 同一个用户并发提交不同金额
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int stake = i + 1;
            executor.execute(() -> {
                stakeService.submitStake(BET_OFFER, 1234, stake);
                latch.countDown();
            });
        }
        latch.await(5, TimeUnit.SECONDS);

        // 验证最高金额是否正确
        String result = stakeService.getHighStakes(BET_OFFER);
        Assert.assertEquals("应保留最大金额", "1234=100", result);
    }

    @Test
    public void testTop20Ordering() {
        // 提交21个不同用户的投注
        for (int i = 1; i <= 21; i++) {
            stakeService.submitStake(BET_OFFER, i, i * 100);
        }
        String[] results = stakeService.getHighStakes(BET_OFFER).split(",");
        Assert.assertEquals("必须返回前20条记录", 20, results.length);
        Assert.assertEquals("第一条应为最高金额", "21=2100", results[0]);
    }
}
