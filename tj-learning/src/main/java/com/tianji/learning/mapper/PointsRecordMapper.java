package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.enums.PointsRecordType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT SUM(points) FROM points_record WHERE user_id  = #{userId} AND type =#{type} AND create_time >= #{begin} AND create_time<= #{end}")
    int queryUserTodayPoint(@Param("userId") Long userId, @Param("type") PointsRecordType type, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("SELECT  type , SUM(points) points  FROM points_record WHERE user_id  = #{userId} AND create_time >= #{begin} AND create_time<= #{end} GROUP BY type")
    List<PointsRecord> queryMyPointToday(@Param("userId") Long userId, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
