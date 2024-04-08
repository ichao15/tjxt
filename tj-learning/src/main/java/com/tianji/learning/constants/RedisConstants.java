package com.tianji.learning.constants;

public interface RedisConstants {

    /**
     * 签到记录的key的前缀：sign:uid:110:202404
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    /**
     * 积分排行榜的key的前缀：boards:202404
     */
    String POINTS_BOARD_KEY_PREFIX = "boards:";

    /**
     * 分片执行的次数
     */
    String XXL_JOB_TIMES = "xxl:job:times";

    /**
     * 分片的总数
     */
    String XXL_SHARD_TOTAL = "xxl:shard:total";
}
