package com.betting.service;

import com.betting.repository.StakeRepository;

/**
 * 处理投注相关的业务逻辑
 *
 * @author dufy
 */
public class StakeService {

    /**
     * 创建 StakeRepository 的实例，用于与数据存储层交互
     */
    private final StakeRepository stakeRepo = StakeRepository.getInstance();

    /**
     * 提交投注金额
     *
     * @param betOfferId 投注项的 ID
     * @param customerId 客户的 ID
     * @param stake 投注金额
     */
    public void submitStake(Integer betOfferId, Integer customerId, int stake) {
        stakeRepo.submitStake(betOfferId, customerId, stake);
    }

    /**
     * 获取指定投注项的最高投注列表
     *
     * @param betOfferId 投注项的 ID
     * @return 返回前 20 名客户及其投注金额的字符串，格式为 "customerId=stake"
     */
    public String getHighStakes(Integer betOfferId) {
        String highStakes = stakeRepo.getHighStakes(betOfferId);
        return highStakes;
    }
}
