package com.betting.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * StakeRepository 类负责管理所有投注数据，采用单例模式并使用线程安全的数据结构来存储和查询投注信息。
 * 该类采用分段锁来减少锁的竞争，从而提高性能。
 * @author dufy
 */
public class StakeRepository {

    private StakeRepository() {}

    /**
     * 单例模式，静态内部类实现线程安全的单例模式
     */
    private static class Holder {
        private static final StakeRepository INSTANCE = new StakeRepository();
    }

    /**
     * 获取 StakeRepository 实例
     * 
     * @return 单例的 StakeRepository 实例
     */
    public static StakeRepository getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 投注项的最高金额列表最大条数
     */
    public static final int MAX_HIGH_STAKES = 20;

    /**
     * 核心存储结构：用于存储每个 betOfferId 对应的投注项数据，使用 static 修饰符确保全局唯一存储结构
     */
    private static final ConcurrentHashMap<Integer, BetOfferData> DATA_STORE = new ConcurrentHashMap<>();

    /**
     *  分段锁配置：根据客户ID的哈希值分配锁，降低锁的竞争
     */
    private static final int LOCK_SEGMENTS = Runtime.getRuntime().availableProcessors() * 4;
    private static final ReentrantLock[] LOCKS = new ReentrantLock[LOCK_SEGMENTS];

    static {
        // 初始化分段锁
        for (int i = 0; i < LOCK_SEGMENTS; i++) {
            LOCKS[i] = new ReentrantLock();
        }
    }

    /**
     * 客户投注数据
     */
    private static class CustomerData {
        /**
         * 用数组存储客户的投注历史，初始容量为4
         */
        int[] stakes = new int[4];
        /**
         * 当前存储的投注记录数量
         */
        int size;
        /**
         * 当前客户的最大投注金额
         */
        int maxStake = Integer.MIN_VALUE;

        /**
         * 非线程安全的方法，需要外部同步来添加投注金额
         * @param stake 投注金额
         */
        void addStake(int stake) {
            // 动态扩容，确保数组能够容纳更多投注金额
            if (size == stakes.length) {
                stakes = Arrays.copyOf(stakes, stakes.length * 2);
            }
            // 将投注金额存入数组
            stakes[size++] = stake;
            // 更新最大投注金额
            if (stake > maxStake) {
                maxStake = stake;
            }
        }
    }

    /**
     * 投注项数据结构，用于存储某一投注项（betOfferId）下的客户投注数据
     */
    private static class BetOfferData {
        // 客户投注记录存储（customerId -> 投注数据）
        final ConcurrentHashMap<Integer, CustomerData> customers = new ConcurrentHashMap<>();

        // 保存20条数据，并发安全的自动排序映射
        final ConcurrentSkipListMap<ComparableEntry, Boolean> top20 = new ConcurrentSkipListMap<>();

        // 自定义Entry
        private static class ComparableEntry implements Comparable<ComparableEntry> {
            /**
             * 客户ID
             */
            final Integer customerId;
            /**
             * 客户的最高投注金额
             */
            final int maxStake;

            ComparableEntry(Integer customerId, int score) {
                this.customerId = customerId;
                this.maxStake = score;
            }

            @Override
            public int compareTo(ComparableEntry o) {
                // 比较规则：首先按投注金额降序排列，若相同，则按客户ID升序排列
                int stakeCompare = Integer.compare(o.maxStake, this.maxStake);
                return stakeCompare != 0 ? stakeCompare : this.customerId.compareTo(o.customerId);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                ComparableEntry that = (ComparableEntry) o;
                return maxStake == that.maxStake && Objects.equals(customerId, that.customerId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(customerId, maxStake);
            }
        }
    }

    /**
     * 提交投注金额，并更新客户的投注历史记录及排行榜，线程安全的，保证在并发环境下正确执行
     * 
     * @param betOfferId 投注项ID
     * @param customerId 客户ID
     * @param stake 投注金额
     */
    public void submitStake(Integer betOfferId, Integer customerId, int stake) {
        // 获取或创建对应的投注项数据
        BetOfferData data = DATA_STORE.computeIfAbsent(betOfferId, k -> new BetOfferData());

        // 获取分段锁，降低锁竞争
        ReentrantLock lock = LOCKS[Math.abs(customerId.hashCode() % LOCK_SEGMENTS)];
        lock.lock();
        try {
            // 获取或创建客户的数据
            CustomerData customer = data.customers.computeIfAbsent(customerId, k -> new CustomerData());

            // 记录客户的投注金额
            customer.addStake(stake);

            // 只有在本次投注金额是该客户的最大值时才更新排行榜
            if (stake == customer.maxStake) {
                // 创建新的条目
                BetOfferData.ComparableEntry newEntry = new BetOfferData.ComparableEntry(customerId, stake);

                // 移除旧的记录（如果存在）
                data.top20.keySet().removeIf(entry -> entry.customerId.equals(customerId));

                // 将新记录插入到前20条的并发映射中
                data.top20.put(newEntry, Boolean.TRUE);

                // 动态处理前20条，确保排名始终保持最新
                while (data.top20.size() > MAX_HIGH_STAKES) {
                    data.top20.pollLastEntry(); // 移除最低分数的条目
                }
            }
        } finally {
            // 锁释放
            lock.unlock();
        }
    }

    /**
     * 获取某个投注项的前20条投注数据
     *
     * @param betOfferId 投注项ID
     * @return 前20条客户及其投注金额的字符串，格式为 "customerId=maxStake"
     */
    public String getHighStakes(Integer betOfferId) {
        BetOfferData data = DATA_STORE.get(betOfferId);
        if (data == null || data.top20.isEmpty()) {
            return "";
        }

        return data.top20.keySet().stream().limit(MAX_HIGH_STAKES)
                .map(entry -> entry.customerId + "=" + entry.maxStake)
                .collect(Collectors.joining(","));
    }

}
