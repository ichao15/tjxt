package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author xyc
 * @since 2024-03-26
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    @Select("SELECT lesson_id id,COUNT(*) num FROM learning_record  WHERE finished = 1 AND finish_time > #{weekBeginTime} AND finish_time < #{weekEndTime} AND user_id  = #{userId} GROUP BY lesson_id")
    List<IdAndNumDTO> countLearnedSections(Long userId, LocalDateTime weekBeginTime, LocalDateTime weekEndTime);
}
