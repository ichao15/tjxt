package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-03-28
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    /**
     * 新增问题
     * @param questionDTO
     */
    void saveQuestion(QuestionFormDTO questionDTO);

    /**
     * 分页查询问题(用户端)
     * @param pageQuery
     * @return
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery);

    /**
     * 根据id查询问题详情
     * @param id
     * @return
     */
    QuestionVO queryQuestionById(Long id);

    /**
     * 分页查询问题(管理端)
     * @param query
     * @return
     */
    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);
}
