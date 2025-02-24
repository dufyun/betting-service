package com.betting.service;

import com.betting.repository.SessionRepository;

/**
 * 处理会话相关的业务逻辑
 *
 * @author dufy
 **/
public class SessionService {

    private final SessionRepository sessionRepo = SessionRepository.getInstance();

    /**
     * 根据给定的 customerId 获取或创建一个新的会话
     *
     * @param customerId 客户的唯一标识符
     * @return 生成或找到的会话键
     */
    public String getOrCreateSession(Integer customerId) {
        return sessionRepo.getOrCreateSession(customerId);
    }

    /**
     * 根据会话键验证会话的有效性
     *
     * @param sessionKey 会话的唯一标识符
     * @return 如果会话有效，返回关联的客户 ID；否则返回 null 或适当的错误码
     */
    public Integer validateSession(String sessionKey) {
        return sessionRepo.findCustomerIdByKey(sessionKey);
    }
}
