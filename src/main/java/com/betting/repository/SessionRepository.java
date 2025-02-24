package com.betting.repository;


import java.time.LocalDate;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.betting.util.SessionKeyUtils;

/**
 * 会话数据存储仓库，提供会话的创建、查询和过期管理
 *
 * @author dufy
 **/
public class SessionRepository {
    /**
     * 分片数量（按CPU核心数优化）
     */
    private static final int SHARD_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 过期时间(毫秒),10分钟
     */
    private static final long SESSION_TIMEOUT = 10 * 60 * 1000;
    /**
     * 清理间隔（秒） 5
     */
    private static final int CLEAN_INTERVAL = 5;

    // 清理线程
    private final ScheduledExecutorService cleaner;
    private final AtomicInteger cleanShardIndex = new AtomicInteger(0);

    /**
     * 分片存储结构（customerId -> session数据）
     */
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Session>> SESSION_SHARDS =
            new ConcurrentHashMap<>(SHARD_COUNT);

    /**
     * 反向索引（使用ConcurrentHashMap保证线程安全）,映射sessionKey -> customerId
     */
    private static final ConcurrentHashMap<String, Integer> SESSION_KEY_TO_CUSTOMER = new ConcurrentHashMap<>(256);

    static {
        // 初始化分片
        for (int i = 0; i < SHARD_COUNT; i++) {
            SESSION_SHARDS.put(i, new ConcurrentHashMap<>(256));
        }
    }

    private SessionRepository() {
        // 优化清理策略：分片轮询清理
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        // 每5秒清理一个分片（全量循环周期=cup核心数*5秒）
        this.cleaner.scheduleAtFixedRate(this::removeExpiredSessions, CLEAN_INTERVAL, CLEAN_INTERVAL, TimeUnit.SECONDS);
    }

    // 单例模式
    private static class Holder {
        private static final SessionRepository INSTANCE = new SessionRepository();
    }

    public static SessionRepository getInstance() {
        return SessionRepository.Holder.INSTANCE;
    }

    private static int hash(Integer key) {
        int h = key.hashCode();
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    /**
     * 根据用户ID创建或获取sessionKey
     *
     * @param customerId 用户ID
     * @return 会话Key
     */
    public String getOrCreateSession(Integer customerId) {
        // 计算分片索引
        int shardIndex = hash(customerId) % SHARD_COUNT;
        ConcurrentHashMap<Integer, Session> shard = SESSION_SHARDS.get(shardIndex);

        Session session = shard.compute(customerId, (k, existing) -> {
            if (existing != null) {
                // 会话级别锁
                synchronized (existing) {
                    if (!existing.isExpired()) {
                        existing.updateAccessTime();
                        return existing;
                    }
                    // 已过期则移除反向索引
                    SESSION_KEY_TO_CUSTOMER.remove(existing.getSessionKey());
                }
            }

            // 创建新会话
            Session newSession = new Session();
            SESSION_KEY_TO_CUSTOMER.put(newSession.getSessionKey(), customerId);
            return newSession;
        });

        return session.getSessionKey();
    }

    /**
     * 根据会话Key查找有效用户ID
     *
     * @param sessionKey 会话Key
     * @return 用户ID（无效或过期返回null）
     */
    public Integer findCustomerIdByKey(String sessionKey) {
        Integer customerId = SESSION_KEY_TO_CUSTOMER.get(sessionKey);
        if (customerId == null) {
            return null;
        }
        // 计算分片索引
        int shardIndex = hash(customerId) % SHARD_COUNT;
        ConcurrentHashMap<Integer, Session> customerToSession = SESSION_SHARDS.get(shardIndex);

        Session session = customerToSession.get(customerId);
        return session.isExpired() ? null : customerId;
    }

    /**
     * 分片轮转清理算法
     * 1. 每次清理一个分片，降低单次处理量
     * 2. 双重检查机制避免误删，同步块保证过期判断与删除的原子性，防止在清理过程中会话被更新
     * 3. 同步清理反向索引，保证数据一致性 ，避免产生孤儿索引
     */
    private void removeExpiredSessions() {
        int currentShard = cleanShardIndex.getAndUpdate(i -> (i + 1) % SHARD_COUNT);
        ConcurrentHashMap<Integer, Session> shard = SESSION_SHARDS.get(currentShard);
        int size = shard.size();
        System.out.println(String.format("清理任务执行时间：%s，执行分片%s ,数量：%s", new Date(), currentShard, size));
        if (size > 0) {
            Iterator<Map.Entry<Integer, Session>> it = shard.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Session> entry = it.next();
                Session session = entry.getValue();
                // 会话级别锁保证原子性
                synchronized (session) {
                    if (session.isExpired()) {
                        // 同步移除反向索引
                        System.out.println(String.format("清理任务执行时间：%s，执行分片%s, Session过期同步移除反向索引:%s ", new Date(),
                            currentShard, session.getSessionKey()));
                        SESSION_KEY_TO_CUSTOMER.remove(session.getSessionKey());
                        // 使用迭代器安全移除
                        it.remove();
                    }
                }
            }
        }
      
    }


    public static class Session {
        private final String sessionKey;
        private volatile long lastAccessedTime;

        Session() {
            this.sessionKey = SessionKeyUtils.generate();
            updateAccessTime();
        }

        void updateAccessTime() {
            this.lastAccessedTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - lastAccessedTime) > SESSION_TIMEOUT;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        @Override
        public String toString() {
            return "Session{" +
                    "sessionKey='" + sessionKey + '\'' +
                    ", lastAccessedTime=" + lastAccessedTime +
                    '}';
        }
    }
}

