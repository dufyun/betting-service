package com.betting.util;

import java.util.concurrent.atomic.AtomicLong;


/**
 * 生成会话 Session Key 的工具类
 *
 * @author dufy
 **/
public class SessionKeyUtils {

    // ------------------- 算法设计细节 -------------------

    /**
     * 时间戳与序列号的组合：
     *
     * 1. 时间戳部分：使用当前时间距离一个预定义基准时间（BASE_TIMESTAMP）的毫秒数，经过左移处理以腾出空间给序列号。
     *    时间戳部分确保了不同时间段内的ID不会重复。
     *    使用44位来表示时间戳，支持约278年的时间范围。
     *
     * 2. 序列号部分：保证同一毫秒内生成的多个ID唯一。使用 AtomicLong 类型的原子操作来进行自增，序列号部分为20位。
     *    每毫秒内最多可以生成104万次ID，足够满足高并发场景需求。
     *
     * 3. Base62 编码：通过将组合后的长整型数值转换为Base62编码，输出8个字符的ID。Base62字符集避免了易混淆的字符（如0、O、I、l等），
     *    确保生成的ID不会发生歧义。
     *
     * 4. 无锁设计：通过原子操作（AtomicLong）实现自增，避免了锁的使用，因此在多线程环境下能够保持高效性能。
     *
     * 5. 容错机制：处理时钟回拨问题。在某些极端情况下，若系统时间回退，则通过增加序列号来弥补。
     *    例如：如果当前时间戳小于上一次生成ID时的时间戳，则调用序列号自增方法来补偿。
     *
     * 6. 性能：该算法能够高效生成ID，不依赖于额外的内存校验，适合高并发的场景。
     *    8位长度的ID能表示约3.5万亿个不同的ID，理论上能维持278年的ID生成。
     */


    // ------------------- 常量定义部分 -------------------

    /**
     * 初始化时间基点（2025-01-01 00:00:00 UTC）。
     * 所有生成的ID都是基于这个时间基点来计算时间戳部分，以减少时区和时间偏差的影响。
     */
    private static final long BASE_TIMESTAMP = 1672531200000L;

    /**
     * 序列号生成器，通过 AtomicLong 保证线程安全，实现无锁自增。
     * 序列号部分每秒可支持约 104 万次调用。
     */
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * Base62 字符集（排除了一些易混淆的字符，确保在编码时没有歧义）。
     * 包含数字（2-9），大写字母（A-Z），小写字母（a-z），共62个字符。
     */
    private static final char[] BASE62_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".toCharArray();

    /**
     *  字符集的大小（62）
     */
    private static final int BASE = BASE62_CHARS.length;

    // ------------------- 主要方法 -------------------

    /**
     * 生成一个唯一的 8 位 Session Key 字符串（线程安全）。
     * 该方法通过将当前时间戳与序列号部分结合，再通过 Base62 编码生成唯一字符串。
     *
     * @return 唯一的8位Session Key
     */
    public static String generate() {
        // 获取当前的时间戳部分，时间戳部分是基于基准时间（BASE_TIMESTAMP）进行偏移的，左移20位确保能够容纳序列号
        long timestampPart = (System.currentTimeMillis() - BASE_TIMESTAMP) << 20;

        // 序列号部分，保证同一毫秒内生成的ID唯一。通过原子操作（AtomicLong）自增序列号，使用& 0xFFFFF 确保不超过20位
        long sequencePart = sequence.getAndIncrement() & 0xFFFFF;

        // 将时间戳部分与序列号部分组合为一个长整型数值
        long combined = timestampPart | sequencePart;

        // 将组合后的长整型数值进行Base62编码，返回唯一的ID
        return encode(combined);
    }

    /**
     * 将长整型数值转换为Base62编码的字符串，输出固定8位长度。
     * 通过对数字进行除法取余操作，将数值转为对应的字符。
     *
     * @param number 需要进行编码的长整型数值
     * @return 编码后的Base62字符串
     */
    private static String encode(long number) {
        // 创建一个字符数组用于存储Base62编码后的字符
        char[] buf = new char[8];

        // 从右到左进行Base62编码
        for (int i = 7; i >= 0; i--) {
            buf[i] = BASE62_CHARS[(int)(number % BASE)];
            number = number / BASE;
        }

        // 返回编码后的字符串
        return new String(buf);
    }

}
