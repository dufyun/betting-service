package com.betting.repository;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
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
     *
     * betOfferId -> ConcurrentSkipListMap, 存储某一投注项（betOfferId）下的客户投注数据，并发安全的自动排序映射 （金额降序 -> 客户ID升序）
     */
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<ComparableEntry>> DATA_STORE = new ConcurrentHashMap<>();

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
     * 自定义Entry
     */
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

    /**
     * 提交投注金额，并更新客户的投注历史记录及排行榜，线程安全的，保证在并发环境下正确执行
     *
     * @param betOfferId 投注项ID
     * @param customerId 客户ID
     * @param stake 投注金额
     */
    public void submitStake(Integer betOfferId, Integer customerId, int stake) {
        // 获取或创建对应的投注项数据
        ConcurrentSkipListSet<ComparableEntry> dataTop20 = DATA_STORE.computeIfAbsent(betOfferId, k -> new ConcurrentSkipListSet());

        // 获取分段锁，降低锁竞争
        ReentrantLock lock = LOCKS[Math.abs(customerId.hashCode() % LOCK_SEGMENTS)];
        lock.lock();
        try {
            // 获取该投注项上客户是否已经投注过
            ComparableEntry existCustomerEntry = dataTop20.stream()
                    .filter(entry -> entry.customerId.equals(customerId))
                    .findFirst()
                    .orElse(null);

            if (existCustomerEntry != null) {
                // 客户投注金额小于当前已投最大金额的时候不需要更新数据
                if (stake <= existCustomerEntry.maxStake) {
                    return;
                }
                // 删除旧的排序记录
                dataTop20.remove(existCustomerEntry);
            }

            // 将新记录插入到前20条的并发映射中
            dataTop20.add(new ComparableEntry(customerId, stake));

            // 动态处理前20条，确保排名始终保持最新
            while (dataTop20.size() > MAX_HIGH_STAKES) {
                dataTop20.pollLast();
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
        ConcurrentSkipListSet<ComparableEntry> dataTop20 = DATA_STORE.get(betOfferId);
        if (dataTop20 == null || dataTop20.isEmpty()) {
            return "";
        }

        return dataTop20.stream().limit(MAX_HIGH_STAKES)
                .map(entry -> entry.customerId + "=" + entry.maxStake)
                .collect(Collectors.joining(","));
    }
}
