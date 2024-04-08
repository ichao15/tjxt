package com.tianji.learning.task;

import lombok.Data;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/03/27  7:59
 */

@Data
public class DelayTask<D> implements Delayed {

    //任务的数据
    private D data;

    //任务执行的时间,单位纳秒
    private long activeTime;


    public DelayTask(D data, Duration delayTime) {
        this.data = data;
        this.activeTime = System.nanoTime()+delayTime.toNanos();
    }

    /**
     * 返回当前任务剩余的时长
     *
     * @param unit
     * @return
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0, activeTime - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    /**
     * 按照剩余的时长进行排序, 值越小越先执行
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Delayed o) {
        long l = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        if (l > 0) {
            return 1;
        } else if (l < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
