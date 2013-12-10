package com.trilemon.boss.poster.recommend.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.taobao.api.domain.Item;
import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.base.service.api.TaobaoApiShopService;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.poster.recommend.PosterRecommendConstants;
import com.trilemon.boss.poster.recommend.PosterRecommendUtils;
import com.trilemon.boss.poster.recommend.dao.PosterRecommendActivityDAO;
import com.trilemon.boss.poster.recommend.dao.PosterRecommendActivityItemDAO;
import com.trilemon.boss.poster.recommend.dao.PosterRecommendUserDAO;
import com.trilemon.boss.poster.recommend.model.PosterRecommendActivity;
import com.trilemon.boss.poster.recommend.model.PosterRecommendActivityItem;
import com.trilemon.boss.poster.recommend.model.PosterRecommendUser;
import com.trilemon.boss.poster.template.client.PosterTemplateClient;
import com.trilemon.boss.poster.template.model.PosterTemplate;
import com.trilemon.commons.web.Page;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static com.trilemon.boss.poster.recommend.PosterRecommendConstants.*;

/**
 * @author kevin
 */
@Service
public class RecommendActivityService {
    private final static Logger logger = LoggerFactory.getLogger(RecommendActivityService.class);
    @Autowired
    private PosterRecommendActivityItemDAO posterRecommendActivityItemDAO;
    @Autowired
    private PosterRecommendActivityDAO posterRecommendActivityDAO;
    @Autowired
    private PosterRecommendUserDAO posterRecommendUserDAO;
    @Autowired
    private AppService appService;
    @Autowired
    private TaobaoApiShopService taobaoApiShopService;
    @Autowired
    private PosterTemplateClient posterTemplateClient;

    /**
     * 获取活动信息
     *
     * @param userId
     * @param activityId
     * @return
     */
    public PosterRecommendActivity getActivity(Long userId, Long activityId) {
        PosterRecommendActivity activity= posterRecommendActivityDAO.selectByUserIdAndActivityId(userId, activityId);
        int itemNum=posterRecommendActivityItemDAO.countByUserIdAndActivityId(userId,activityId);
        activity.setItemNum(itemNum);
        return activity;
    }

    /**
     * 创建活动（未投放）
     *
     * @param userId
     * @param activity
     */
    @Transactional
    public void createActivity(Long userId, PosterRecommendActivity activity) {
        createUser(userId);

        activity.setUserId(userId);
        activity.setAddTime(appService.getLocalSystemTime().toDate());
        activity.setStatus(PosterRecommendConstants.ACTIVITY_ITEM_STATUS_NORMAL);

        //生成最终模板
        PosterTemplate posterTemplate = posterTemplateClient.getPosterTemplate(activity.getTemplateId());
        String templateFtl = posterTemplate.getTemplateFtl();
        List<PosterRecommendActivityItem> activityItems = posterRecommendActivityItemDAO.selectByUserIdAndActivityId
                (userId, activity.getId());

        String publishHtml = generateActivityPublishHtml(templateFtl, activityItems);
        activity.setPublishHtml(publishHtml);

        Long id = posterRecommendActivityDAO.insertSelective(userId, activity);
        logger.info("add activity, activityId[{}] userId[{}].", id, userId);
    }

    /**
     * 生成活动代码
     *
     * @param templateFtl
     * @param activityItems
     * @return
     */
    private String generateActivityPublishHtml(String templateFtl, List<PosterRecommendActivityItem> activityItems) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * 创建用户
     *
     * @param userId
     */
    public void createUser(Long userId) {
        PosterRecommendUser posterRecommendUser = posterRecommendUserDAO.selectByUserId(userId);
        if (null != posterRecommendUser) {
            posterRecommendUser.setAddTime(appService.getLocalSystemTime().toDate());
            posterRecommendUser.setStatus(USER_STATUS_NORMAL);
            posterRecommendUser.setUserId(userId);
            posterRecommendUserDAO.insertSelective(posterRecommendUser);
            logger.info("add user, userId[{}].", userId);
        } else {
            logger.info("add user, userId[{}] exist, skip.", userId);
        }
    }

    /**
     * 更新模板位置
     *
     * @param userId
     * @param activityId
     * @param detailPagePosition
     * @return 是否更新成功
     */
    public boolean updateActivityDetailPagePosition(Long userId, Long activityId, Byte detailPagePosition) {
        PosterRecommendActivity posterRecommendActivity = posterRecommendActivityDAO.selectByUserIdAndActivityId(userId, activityId);
        if (null != posterRecommendActivity) {
            PosterRecommendActivity newPosterRecommendActivity = new PosterRecommendActivity();
            newPosterRecommendActivity.setId(activityId);
            newPosterRecommendActivity.setUserId(userId);
            newPosterRecommendActivity.setDetailPagePosition(detailPagePosition);
            return posterRecommendActivityDAO.updateByUserIdAndActivityId(newPosterRecommendActivity) == 1;
        } else {
            return false;
        }
    }

    /**
     * 删除活动
     *
     * @param userId
     * @param activityId
     * @return
     */
    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        posterRecommendActivityItemDAO.deleteByUserIdAndActivityId(userId, activityId);
        posterRecommendActivityDAO.deleteByUserIdAndActivityId(userId, activityId);
    }

    /**
     * 获取投放代码
     *
     * @param userId
     * @param activityId
     * @return 没有则返回空字符串
     */
    public String getActivityPublishHtml(Long userId, Long activityId) {
        PosterRecommendActivity posterRecommendActivity = posterRecommendActivityDAO.selectByUserIdAndActivityId(userId, activityId);
        return null != posterRecommendActivity ? posterRecommendActivity.getPublishHtml() : "";
    }

    /**
     * 添加宝贝到活动模板
     *
     * @param userId
     * @param activityId
     * @param posterRecommendActivityItem
     */
    public String addActivityItem(Long userId, Long activityId, PosterRecommendActivityItem
            posterRecommendActivityItem) throws IOException {
        PosterRecommendActivity activity = posterRecommendActivityDAO.selectByUserIdAndActivityId(userId, activityId);
        //生成投放的 html 代码
        PosterTemplate template = posterTemplateClient.getPosterTemplate(activity.getTemplateId());

        StringTemplateLoader stringLoader = new StringTemplateLoader();
        String firstTemplate = "firstTemplate";
        stringLoader.putTemplate(firstTemplate, template.getTemplateSlotFtl());
        Configuration cfg = new Configuration();
        cfg.setTemplateLoader(stringLoader);
        Template ftlTemplate = cfg.getTemplate(firstTemplate);
        Writer out = new StringBuilderWriter();
        Map<String, Object> maps = Maps.newHashMap();
        try {
            ftlTemplate.process(maps, out);
            String result = out.toString();
        } catch (TemplateException e) {
            logger.error("", e);
        } finally {
            out.close();
        }

        //String itemPreviewHtml=PosterRecommendUtils.posterRecommendActivityItem2Item(posterRecommendActivityItem);
        posterRecommendActivityItem.setUserId(userId);
        posterRecommendActivityItem.setActivityId(activityId);
        posterRecommendActivityItem.setStatus(ACTIVITY_ITEM_STATUS_NORMAL);
        posterRecommendActivityItem.setAddTime(appService.getLocalSystemTime().toDate());
        posterRecommendActivityItemDAO.insertSelective(posterRecommendActivityItem);
        return "";
    }

    /**
     * 从活动模板中移除宝贝
     *
     * @param userId
     * @param activityId
     * @param itemNumIid
     */
    public int removeActivityItem(Long userId, Long activityId, Long itemNumIid) {
        return posterRecommendActivityItemDAO.deleteByUserIdAndActivityIdAndItemNumIid(userId, activityId, itemNumIid);
    }

    /**
     * 查询已加入活动宝贝
     *
     * @param userId
     * @param activityId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public Page<PosterRecommendActivityItem> paginateActivityAddedItems(Long userId, Long activityId, int pageNum, int pageSize) {
        return posterRecommendActivityItemDAO.paginateByUserIdAndActivityId(userId, activityId, (pageNum - 1) * pageSize, pageSize);
    }

    /**
     * 在售宝贝，以供加入活动
     *
     * @param userId
     * @param onSale
     * @param query
     * @param sellerCids
     * @param pageNum
     * @param pageSize
     * @return
     */
    public Page<PosterRecommendActivityItem> paginateCanBeAddActivityItems(final Long userId,
                                                                           final Long activityId,
                                                                           boolean onSale,
                                                                           String query,
                                                                           List<Long> sellerCids,
                                                                           int pageNum,
                                                                           int pageSize) throws TaobaoAccessControlException, TaobaoEnhancedApiException, TaobaoSessionExpiredException {
        Page<Item> itemPage;
        List<PosterRecommendActivityItem> activityItems;
        if (onSale) {
            itemPage = taobaoApiShopService.paginateOnSaleItems(userId, query,
                    ITEM_FIELDS, sellerCids, pageNum, pageSize, true, true, "modified:desc");
            activityItems = PosterRecommendUtils.items2PosterRecommendActivityItems
                    (userId, activityId, itemPage.getItems());
            return Page.create(itemPage.getTotalSize(), itemPage.getPageNum(), itemPage.getPageSize(), activityItems);
        } else {
            itemPage = taobaoApiShopService.paginateInventoryItems(userId, query,
                    ITEM_FIELDS, Lists.newArrayList(BANNER_OFF_SHELF), sellerCids, pageNum, pageSize, true, "modified:desc");
            activityItems = PosterRecommendUtils.items2PosterRecommendActivityItems
                    (userId, activityId, itemPage.getItems());
            return Page.create(itemPage.getTotalSize(), itemPage.getPageNum(), itemPage.getPageSize(), activityItems);
        }
    }

    /**
     * 获取最近创建的模板
     *
     * @param userId
     * @return
     */
    public PosterTemplate getLastUsedPosterTemplate(Long userId) {
        PosterRecommendActivity posterRecommendActivity = posterRecommendActivityDAO.selectLastCreatedActivity(userId);
        if (null != posterRecommendActivity) {
            Long templateId = posterRecommendActivity.getTemplateId();
            return posterTemplateClient.getPosterTemplate(templateId);
        } else {
            return null;
        }
    }
}