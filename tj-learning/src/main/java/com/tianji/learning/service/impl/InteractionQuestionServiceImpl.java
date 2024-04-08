package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-03-28
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final CourseClient courseClient;
    private final CategoryCache categoryCache;


    @Override
    public void saveQuestion(QuestionFormDTO dto) {
        if (ObjectUtils.isEmpty(dto)) {
            return;
        }
        // 1. 获得当前用户
        Long userId = UserContext.getUser();
        // 2. 把questionDTO转成InteractionQuestion
        InteractionQuestion question = BeanUtils.copyBean(dto, InteractionQuestion.class);
        // 3. 设置userId
        question.setUserId(userId);
        // 4. 保存
        save(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {

        Long userId = UserContext.getUser();

        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        Boolean onlyMine = query.getOnlyMine();

        //1. 健壮性判断
        if (courseId == null && sectionId == null) {
            throw new BizIllegalException("参数有误, courseId和sectionId不能同时为null");
        }

        //2. 分页查询 hidden为false的问题, 根据前端的条件进行过滤
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, q -> !q.getProperty().equals("description")) //InteractionQuestion的属性如果不是description,才查出来
                .eq(InteractionQuestion::getHidden, false)
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(BooleanUtils.isTrue(onlyMine), InteractionQuestion::getUserId, userId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        List<InteractionQuestion> questionList = page.getRecords();
        if (CollUtils.isEmpty(questionList)) {
            return PageDTO.empty(page);
        }

        // 3.根据latest_answer_id批量查询最近一次回答信息 answerIds一个集合 集合自身
        Set<Long> answerIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (InteractionQuestion question : questionList) {
            answerIds.add(question.getLatestAnswerId());
            //不是匿名的 用户信息才需要查询(提问者的用户id)
            if (!question.getAnonymity()) {
                userIds.add(question.getUserId());
            }
        }

        //有的问题是没有最新一次回答的, 就是null, 移除
        answerIds.remove(null);
        List<InteractionReply> replyList = new ArrayList<>();
        //如果这个问题整个没人回答, answerIds不是null,但是长度为0, 数据库查询会报错
        if (CollUtils.isNotEmpty(answerIds)) {
            replyList = replyMapper.selectBatchIds(answerIds);
        }

        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtils.isNotEmpty(replyList)) {
            for (InteractionReply reply : replyList) {
                replyMap.put(reply.getId(), reply);
                //不是匿名回答的用户, 才需要查询用户信息
                if (!reply.getAnonymity()) {
                    userIds.add(reply.getUserId());
                }
            }
        }

        //4. 根据user_id批量查询用户（提问者和最近一次回答用户）信息
        userIds.remove(null);
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (CollUtils.isNotEmpty(userList)) {
            userMap = userList.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        //5. 封装QuestionVO
        List<QuestionVO> voList = new ArrayList<>();
        for (InteractionQuestion question : questionList) {
            //每遍历一次 就需要负责成一个QuestionVO
            //5.1 基本信息
            QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
            //5.2 封装提问者信息
            if (!question.getAnonymity()) {
                UserDTO user = userMap.get(question.getUserId());
                if (user != null) {
                    vo.setUserName(user.getUsername());
                    vo.setUserIcon(user.getIcon());
                }
            }

            //5.3 封装最后一次回答信息
            InteractionReply reply = replyMap.get(question.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                //不是匿名回答
                if (!reply.getAnonymity()) {
                    UserDTO user = userMap.get(reply.getUserId());
                    if (user != null) {
                        vo.setLatestReplyUser(user.getUsername());
                    }
                }
            }
            voList.add(vo);
        }

        //6. 返回
        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        //1.根据id查询InteractionQuestion
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new DbException("查询的question不存在");
        }
        QuestionVO questionVO = BeanUtils.copyBean(question, QuestionVO.class);
        //2.判断是否是匿名的, 不是,封装提问者信息
        if(!question.getAnonymity()){
            Long userId = question.getUserId();
            if (userId != null) {
                UserDTO userDTO = userClient.queryUserById(userId);
                if (userDTO != null) {
                    questionVO.setUserName(userDTO.getUsername());
                    questionVO.setUserIcon(userDTO.getIcon());
                }
            }
        }
        return questionVO;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        String courseName = query.getCourseName();
        Integer status = query.getStatus();
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();

        List<Long> courseIds = null;
        //1.处理课程名称,获得课程id
        if (StringUtils.isNotEmpty(courseName)) {
            courseIds = searchClient.queryCoursesIdByName(courseName);
            if(CollectionUtils.isEmpty(courseIds)){
                return PageDTO.empty(0L,0L);
            }
        }

        //2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .ge(beginTime != null, InteractionQuestion::getCreateTime, beginTime)
                .le(endTime != null, InteractionQuestion::getCreateTime, endTime)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        List<InteractionQuestion> questionList = page.getRecords();
        if(CollUtils.isEmpty(questionList)){
            return PageDTO.empty(page);
        }

        //3.准备vo需要的数据(用户数据,章节数据,课程数据)
        Set<Long> userIds = new HashSet<>();
        Set<Long> catalogIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        for (InteractionQuestion question : questionList) {
            userIds.add(question.getUserId());
            // 章、节同一个表
            catalogIds.add(question.getChapterId());
            catalogIds.add(question.getSectionId());
            cIds.add(question.getCourseId());
        }

        //3.1查询用户数据
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>();
        if(CollUtils.isNotEmpty(userList)){
            userMap = userList.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        //3.2查询课程数据
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> courseMap = new HashMap<>();
        if(CollUtils.isNotEmpty(courseList)){
            courseMap = courseList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        //3.3查询章节数据
        List<CataSimpleInfoDTO> cataList = catalogueClient.batchQueryCatalogue(catalogIds);
        Map<Long, CataSimpleInfoDTO> cataMap = new HashMap<>();
        if(CollUtils.isNotEmpty(cataList)){
            cataMap = cataList.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, c -> c));
        }

        //4.封装vo
        List<QuestionAdminVO> questionAdminVOList = new ArrayList<QuestionAdminVO>();
        for (InteractionQuestion question : questionList) {
            QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);
            //4.1封装用户数据
            UserDTO userDTO = userMap.get(question.getUserId());
            if(userDTO!=null){
                vo.setUserName(userDTO.getUsername());
            }

            //4.2封装课程数据和类别数据
            CourseSimpleInfoDTO course = courseMap.get(question.getCourseId());
            if(course!=null){
                vo.setCourseName(course.getName());
                String categoryNames = categoryCache.getCategoryNames(course.getCategoryIds());
                vo.setCategoryName(categoryNames);
            }

            //4.3封装章节数据
            CataSimpleInfoDTO chapter = cataMap.get(question.getChapterId());
            if(chapter!=null){
                vo.setChapterName(chapter.getName());
            }

            CataSimpleInfoDTO section = cataMap.get(question.getSectionId());
            if(section!=null){
                vo.setSectionName(section.getName());
            }

            questionAdminVOList.add(vo);
        }

        return PageDTO.of(page,questionAdminVOList);
    }
}
