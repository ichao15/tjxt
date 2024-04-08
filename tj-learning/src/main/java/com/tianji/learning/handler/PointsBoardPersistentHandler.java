package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/08  7:24
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {

    private final IPointsBoardSeasonService seasonService;

    private final IPointsBoardService boardService;

    private final StringRedisTemplate redisTemplate;

    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason() {
        //1. 创建定时任务createTableJob
        //2. 获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //3. 查询赛季id
        PointsBoardSeason season = seasonService.querySessionByTime(time);
        if(season==null){
            return;
        }
        //4. 创建表
        boardService.createPointsBoardTableBySeason(season);
    }

    //持久化到Mysql
    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB() {
        //1. 获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //2. 根据上月时间,查询赛季, 计算动态表名,存到ThreadLocal
        PointsBoardSeason sesson = seasonService
                .lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        if (sesson == null) {
            return;
        }
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + sesson.getId();
        TableInfoContext.setInfo(tableName);

        int index = XxlJobHelper.getShardIndex(); // 0,1,2
        int total = XxlJobHelper.getShardTotal(); // 3
        log.info("index="+index);
        log.info("total="+total);

        //3. 拼接redis的key, 从Redis中查询榜单数据
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int pageNo = index + 1;
        int pageSize = 100;

        while (true) {
            List<PointsBoard> boardList = queryCurrentBoardList(key, pageNo, pageSize);
            // redis里面都取完了, 取不到,结束
            if(CollUtils.isEmpty(boardList)){
                break;
            }
            //4. 持久化
            boardList.forEach(b->{
                b.setId(b.getRank().longValue()); //id作为排名
                b.setRank(null);
            });

            boardService.saveBatch(boardList);
            pageNo += total;
        }

        //5. 移除ThreadLocal
        TableInfoContext.remove();

        // 存  此处是为了防止在分布式场景下， 一个任务分片执行完后就马上执行子任务。
        redisTemplate.opsForValue().increment(RedisConstants.XXL_JOB_TIMES); // 分片执行的次数
        redisTemplate.opsForValue().set(RedisConstants.XXL_SHARD_TOTAL, String.valueOf(total)); // 分片的总数  3
    }

    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        String jobTimes = redisTemplate.opsForValue().get(RedisConstants.XXL_JOB_TIMES);
        String shardTotal = redisTemplate.opsForValue().get(RedisConstants.XXL_SHARD_TOTAL);
        // 判断，等三个分片都执行完成之后，再去删除
        if (jobTimes != null && jobTimes.equals(shardTotal)) {
            //1. 获取上月时间
            LocalDateTime time = LocalDateTime.now().minusMonths(1);
            //2. 计算key
            String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateTimeFormatter.ofPattern("yyyyMM"));
            //3. 使用unlink删除
            redisTemplate.unlink(key);

            redisTemplate.delete(RedisConstants.XXL_JOB_TIMES);
            redisTemplate.delete(RedisConstants.XXL_SHARD_TOTAL);
        }
    }

    private List<PointsBoard> queryCurrentBoardList(String key, int pageNo, int pageSize) {
        BoundZSetOperations<String, String> zSetOps = redisTemplate.boundZSetOps(key);

        //1.分页查询
        int start = (pageNo - 1) * pageSize;
        int end = start + pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> tuples = zSetOps.reverseRangeWithScores(start, end);

        //2. 遍历封装
        if (CollUtils.isEmpty(tuples)) {
            return null;
        }

        int rank = start + 1;
        List<PointsBoard> boardList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Double point = tuple.getScore();
            String userId = tuple.getValue();

            if (userId == null || point == null) {
                continue;
            }

            PointsBoard board = new PointsBoard();
            board.setUserId(Long.valueOf(userId));
            board.setPoints(point.intValue());
            board.setRank(rank);
            boardList.add(board);

            rank++;
        }

        return boardList;
    }
}
