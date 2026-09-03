package com.ruoyi.vqms.ingestion;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;

/**
 * 重算单飞互斥：任何时刻至多一个重算操作在跑（手动端点 × Quartz 夜任务防撞车）。
 *
 * 场景：白天手动重算与 03:00 夜任务并发、或连续点击——各管线行级 upsert 幂等可自愈，
 * 但考核数据挂钩资金，聚合交叉写会产生陈旧窗口；撞车时后来者立即拒绝（不排队），
 * 提示稍后重试。Quartz 任务拿不到锁同样跳过本次（次日缺口补算自愈，见 VqmsStatsTask）。
 */
@Component
public class RecomputeLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    /** 独占执行；已被占用时抛 ServiceException（HTTP 409 语义）。 */
    public <T> T guard(Supplier<T> action) {
        if (!lock.tryLock()) {
            throw new ServiceException("重算进行中（另一手动触发或夜间任务在跑），请稍后再试");
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
