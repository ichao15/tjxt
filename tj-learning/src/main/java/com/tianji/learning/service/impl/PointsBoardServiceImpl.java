package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + LocalDateTime.now().format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        Long season = query.getSeason();
        //1. 判断是否是当前赛季
        boolean isCurrent = season == null || season == 0;
        //2. 查询当前用户的榜单
        PointsBoard myBoard = isCurrent ? queryMyCurrentBoard(key) : queryHistoryBoard(query);
        //3. 分页查询排行榜
        List<PointsBoard> boardList = isCurrent ? queryCurrentBoardList(key, query.getPageNo(), query.getPageSize()) : queryHistoryBoardList(query);
        //4. 封装PointsBoardVO
        PointsBoardVO vo = new PointsBoardVO();
        //4.1封装我的排名和积分
        if (myBoard != null) {
            vo.setRank(myBoard.getRank());
            vo.setPoints(myBoard.getPoints());
        }
        //4.2封装boardList
        if (CollUtils.isEmpty(boardList)) {
            return vo;
        }
        //4.2.1 查询用户Id用户信息
        Set<Long> userIds = boardList.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        Map<Long, String> userMap = new HashMap<>();
        if (userList != null) {
            userMap = userList.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }

        //4.2.2 转换PointsBoardItemVO
        List<PointsBoardItemVO> boardItemList = new ArrayList<>();
        for (PointsBoard pointsBoard : boardList) {
            PointsBoardItemVO boardItem = new PointsBoardItemVO();
            boardItem.setPoints(pointsBoard.getPoints());
            boardItem.setRank(pointsBoard.getRank());
            boardItem.setName(userMap.get(pointsBoard.getUserId()));

            boardItemList.add(boardItem);
        }
        vo.setBoardList(boardItemList);

        return vo;
    }

    @Override
    public void createPointsBoardTableBySeason(PointsBoardSeason season) {
        this.baseMapper.createPointsBoardTableBySeason(LearningConstants.POINTS_BOARD_TABLE_PREFIX + season.getId());
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        BoundZSetOperations<String, String> zSetOps = redisTemplate.boundZSetOps(key);
        PointsBoard myBoard = new PointsBoard();
        //1.查询我的排名
        Long rank = zSetOps.reverseRank(UserContext.getUser().toString());
        myBoard.setRank(rank == null ? 0 : rank.intValue() + 1);

        //2.查询我的积分
        Double score = zSetOps.score(UserContext.getUser().toString());
        myBoard.setPoints(score == null ? 0 : score.intValue());

        return myBoard;
    }

    private List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        BoundZSetOperations<String, String> zSetOps = redisTemplate.boundZSetOps(key);

        //1. 计算 start end 0，9  10，19
        int start = (pageNo - 1) * pageSize;
        int end = start + pageSize - 1;

        //2. 从Redis查询  ZREVRANGE key start stop [WITHSCORES]
        Set<ZSetOperations.TypedTuple<String>> tupleSet = zSetOps.reverseRangeWithScores(start, end);
        if (CollUtils.isEmpty(tupleSet)) {
            return CollUtils.emptyList();
        }

        //3. 封装List<PointsBoard>
        int rank = start + 1;
        List<PointsBoard> boardList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tupleSet) {
            String userId = tuple.getValue();
            Double point = tuple.getScore();

            if (userId == null || point == null) {
                continue;
            }

            PointsBoard board = new PointsBoard();
            board.setUserId(Long.valueOf(userId));
            board.setPoints(point.intValue());
            board.setRank(rank++);
            boardList.add(board);
        }
        return boardList;
    }

    //查询历史赛季我的排名
    private PointsBoard queryHistoryBoard(PointsBoardQuery query) {
        //1.拼接tableName
        Long seasonId = query.getSeason();
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX+seasonId;
        TableInfoContext.setInfo(tableName);
        //2.查询
        PointsBoard pointsBoard = lambdaQuery()
                .eq(PointsBoard::getUserId, UserContext.getUser())
                .one();

        if(pointsBoard==null){
            return null;
        }

        pointsBoard.setRank(pointsBoard.getId().intValue());
        //3.移除ThreadLocal
        TableInfoContext.remove();
        return pointsBoard;
    }

    //查询历史赛季排行榜
    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        //1.拼接tableName
        Long seasonId = query.getSeason();
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX+seasonId;
        TableInfoContext.setInfo(tableName);
        //2.分页查询
        Page<PointsBoard> page = page(query.toMpPage());

        //3.数据处理
        List<PointsBoard> pointsBoardList = page.getRecords();
        if (CollUtils.isEmpty(pointsBoardList)) {
            return CollUtils.emptyList();
        }
        for (PointsBoard record : pointsBoardList) {
            record.setRank(record.getId().intValue());
        }
        //4.移除ThreadLocal
        TableInfoContext.remove();
        return pointsBoardList;
    }
}
