package com.trilemon.boss.inventory.service;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.taobao.api.domain.Item;
import com.taobao.api.request.ItemsInventoryGetRequest;
import com.taobao.api.request.ItemsOnsaleGetRequest;
import com.taobao.api.response.ItemsInventoryGetResponse;
import com.taobao.api.response.ItemsOnsaleGetResponse;
import com.trilemon.boss.center.PlanDistributionUtils;
import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.base.service.api.TaobaoApiShopService;
import com.trilemon.boss.infra.base.service.api.exception.BaseTaobaoApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.infra.base.util.TopApiUtils;
import com.trilemon.boss.inventory.InventoryConstants;
import com.trilemon.boss.inventory.InventoryException;
import com.trilemon.boss.inventory.InventoryUtils;
import com.trilemon.boss.inventory.dao.InventoryListItemMapper;
import com.trilemon.boss.inventory.dao.InventoryListSettingMapper;
import com.trilemon.boss.inventory.model.InventoryListItem;
import com.trilemon.boss.inventory.model.InventoryListSetting;
import com.trilemon.boss.inventory.model.dto.InventoryItem;
import com.trilemon.commons.DateUtils;
import com.trilemon.commons.LocalTimeInterval;
import com.trilemon.commons.mybatis.MyBatisBatchWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.trilemon.boss.inventory.InventoryConstants.SETTING_STATUS_RUNNING;
import static com.trilemon.boss.inventory.InventoryUtils.buildListItem;
import static com.trilemon.commons.Collections3.COMMA_SPLITTER;

/**
 * @author kevin
 */
@Service
public class InventoryListAdjustService {
    private final static Logger logger = LoggerFactory.getLogger(InventoryListAdjustService.class);
    @Autowired
    private InventoryListSettingMapper inventoryListSettingMapper;
    @Autowired
    private InventoryListItemMapper inventoryListItemMapper;
    @Autowired
    private TaobaoApiShopService taobaoApiShopService;
    @Autowired
    private AppService appService;
    @Autowired
    private MyBatisBatchWriter myBatisBatchWriter;

    public void update(Long userId) throws InventoryException, TaobaoAccessControlException, TaobaoEnhancedApiException, TaobaoSessionExpiredException {
        checkNotNull(userId, "userId is not null", userId);

        InventoryListSetting setting = inventoryListSettingMapper.selectByUserId(userId);
        if (null == setting) {
            throw new InventoryException("userId[" + userId + "] setting is null");
        }

        //所有仓库宝贝
        List<InventoryItem> inventoryItems = Lists.newArrayList();
        if (null == setting.getIncludeBanners()) {
            logger.info("userId[" + userId + "] banners is null.");
            return;
        }

        List<String> banners = COMMA_SPLITTER.splitToList(setting.getIncludeBanners());
        for (String banner : banners) {
            ItemsInventoryGetRequest request = new ItemsInventoryGetRequest();
            request.setFields(Joiner.on(",").join(InventoryConstants.ITEM_FIELDS));
            request.setBanner(banner);
            ItemsInventoryGetResponse result = taobaoApiShopService.getInventoryItems(userId, request);
            List<Item> items = result.getItems();
            if (CollectionUtils.isNotEmpty(items)) {
                for (Item item : items) {
                    InventoryItem inventoryItem = new InventoryItem();
                    inventoryItem.setItem(item);
                    inventoryItem.setBanner(banner);
                    inventoryItems.add(inventoryItem);
                }
            }
        }

        if (CollectionUtils.isEmpty(inventoryItems)) {
            logger.info("inventory item is empty, userId[{}]", userId);
            return;
        }

        Set<Long> inventoryItemNumIids = Sets.newHashSet(InventoryUtils.getInventoryItemNumIids(inventoryItems));


        //所有计划中宝贝
        List<InventoryListItem> runningListItems = inventoryListItemMapper.selectBySettingId(setting.getId());
        final List<Long> runningListItemNumIids = InventoryUtils.getInventoryListItemNumIids(runningListItems);

        //正在计划中的并且在库存的宝贝NumIid
        Set<Long> existAndInventoryItemNumIids = Sets.intersection(Sets.newHashSet(inventoryItemNumIids),
                Sets.newHashSet(runningListItemNumIids));

        //计划中失效宝贝NumIid
        List<Long> invalidListItemNumIids = ListUtils.removeAll(runningListItemNumIids,
                existAndInventoryItemNumIids);

        //需要加入计划的新加入宝贝NumIid
        List<Long> newItemNumIids = ListUtils.removeAll(inventoryItemNumIids, existAndInventoryItemNumIids);

        //需要加入计划的新加入宝贝
        List<InventoryItem> newInventoryItems = InventoryUtils.getInventoryItems(inventoryItems, newItemNumIids);

        //1. 删除计划中失效宝贝
        if (CollectionUtils.isNotEmpty(invalidListItemNumIids)) {
            inventoryListItemMapper.deleteByNumIids(userId, invalidListItemNumIids);
        }

        //2. 加入新宝贝
        if (CollectionUtils.isEmpty(newInventoryItems)) {
            return;
        }
        List<InventoryListItem> validInventoryListItems = InventoryUtils.getInventoryListItem(runningListItems,
                existAndInventoryItemNumIids);
        Table<Integer, LocalTimeInterval, Integer> currDistribution = PlanDistributionUtils.getDistribution(validInventoryListItems);
        //为新添宝贝安排具体的调整时间
        Table<Integer, LocalTimeInterval, List<InventoryItem>> assignTable = avgAssignNewItems(newInventoryItems,
                setting,
                currDistribution);
        //安排计划
        List<InventoryListItem> plans = plan(setting, assignTable);

        savePlan(setting, plans);
    }

    /**
     * 安排计划并保存
     * @param userId
     * @param setting
     * @throws InventoryException
     * @throws TaobaoSessionExpiredException
     * @throws TaobaoAccessControlException
     * @throws TaobaoEnhancedApiException
     */
    public void createPlan(Long userId, InventoryListSetting setting) throws InventoryException,
            TaobaoSessionExpiredException,
            TaobaoAccessControlException, TaobaoEnhancedApiException {
        checkArgument(userId.equals(setting.getUserId()), "userId[%s] is not equal with userId of planSetting[%s]",
                userId, setting.getUserId());
        ItemsOnsaleGetRequest request = new ItemsOnsaleGetRequest();
        request.setFields(Joiner.on(",").join(InventoryConstants.ITEM_FIELDS));
        request.setSellerCids(setting.getIncludeSellerCids());
        ItemsOnsaleGetResponse result = taobaoApiShopService.getOnSaleItems(userId, request);

        //获取已经计划的宝贝
        List<Long> usedItemNumIids = inventoryListItemMapper.selectNumIidsByUserId(userId);

        //排除宝贝
        List<Item> excludeItems = TopApiUtils.excludeItems(result.getItems(), usedItemNumIids);
        List<InventoryListItem> plans = plan(setting, InventoryUtils.getInventoryItems(excludeItems));
        logger.info("userId[{}] generate {} plans for planSettingId[{}].", userId, plans.size(), setting.getId());
        savePlan(setting, plans);
    }

    /**
     * @param setting
     * @param inventoryItems
     * @return
     * @throws InventoryException
     */
    public List<InventoryListItem> plan(InventoryListSetting setting, List<InventoryItem> inventoryItems) throws
            InventoryException {
        Table<Integer, LocalTimeInterval, List<InventoryItem>> assignTable = null;
        switch (setting.getListType()) {
            case InventoryConstants.LIST_TYPE_AVG:
                assignTable = avgAssignItems(setting, inventoryItems);
                break;
        }
        if (null == assignTable) {
            throw new InventoryException("userId[" + setting.getUserId() + "] assign table is null.");
        }
        return plan(setting, assignTable);
    }

    /**
     * 均匀分配
     * @param setting
     * @param inventoryItems
     * @return
     * @throws InventoryException
     */
    private Table<Integer, LocalTimeInterval, List<InventoryItem>> avgAssignItems(InventoryListSetting setting,
                                                                                  List<InventoryItem> inventoryItems)
            throws InventoryException {
        Table<Integer, LocalTimeInterval, Integer> distribution;
        try {
            distribution = PlanDistributionUtils.parseAndFillZeroDistribution(setting.getDistribution());
        } catch (Exception e) {
            throw new InventoryException("parse distribution error, planSettingId[" + setting.getId() + "]", e);
        }
        return assignItems(inventoryItems, distribution);
    }

    private Table<Integer, LocalTimeInterval, List<InventoryItem>> avgAssignNewItems(List<InventoryItem> inventoryItems,
                                                                                     InventoryListSetting setting,
                                                                                     Table<Integer, LocalTimeInterval, Integer> currDistribution)
            throws InventoryException {
        try {
            Table<Integer, LocalTimeInterval, Integer> planDistribution = PlanDistributionUtils.parseAndFillZeroDistribution(setting.getDistribution());
            Table<Integer, LocalTimeInterval, Integer> newItemDistribution = null;
            switch (setting.getListType()) {
                case InventoryConstants.LIST_TYPE_AVG:
                    newItemDistribution = PlanDistributionUtils.getNewItemDistribution(inventoryItems.size(),
                            planDistribution, currDistribution);
                    break;
            }
            if (null == newItemDistribution) {
                throw new InventoryException("userId[" + setting.getUserId() + "] newItemDistribution is null.");
            }
            return assignItems(inventoryItems, newItemDistribution);
        } catch (Exception e) {
            throw new InventoryException("parse distribution error, settingId[" + setting.getId() + "]", e);
        }
    }

    private Table<Integer, LocalTimeInterval, List<InventoryItem>> assignItems(List<InventoryItem> inventoryItems,
                                                                               Table<Integer, LocalTimeInterval, Integer> newItemDistribution) {
        List<Item> items = InventoryUtils.getItems(inventoryItems);
        Table<Integer, LocalTimeInterval, List<Item>> assignItems = PlanDistributionUtils.assignItems(items,
                newItemDistribution);
        Table<Integer, LocalTimeInterval, List<InventoryItem>> assignInventoryItems = HashBasedTable.create();
        for (Table.Cell<Integer, LocalTimeInterval, List<Item>> cell : assignItems.cellSet()) {
            assignInventoryItems.put(cell.getRowKey(), cell.getColumnKey(), InventoryUtils.getInventoryItems(cell.getValue()));
        }
        return assignInventoryItems;
    }

    public List<InventoryListItem> plan(InventoryListSetting setting, Table<Integer, LocalTimeInterval,
            List<InventoryItem>> assignTable) {
        List<InventoryListItem> listItems = Lists.newArrayList();

        DateTime now = appService.getLocalSystemTime();
        DateTime tomorrow = now.plusDays(1).withTimeAtStartOfDay();
        //第一次调整是周几
        DateTime firstAdjustDay = now;
        //如果离第二天小于10分钟，就把首次调整计划安排到第二天
        if (Minutes.minutesBetween(now, tomorrow).getMinutes() < 10) {
            firstAdjustDay = now.plusDays(1);
        }
        int firstAdjustDayOfWeek = firstAdjustDay.getDayOfWeek();
        for (Map.Entry<Integer, Map<LocalTimeInterval, List<InventoryItem>>> assignDay : assignTable.rowMap().entrySet()) {
            for (Map.Entry<LocalTimeInterval, List<InventoryItem>> assignHour : assignDay.getValue().entrySet()) {
                LocalTimeInterval assignHourTimeInterval = assignHour.getKey();
                List<InventoryItem> items = assignHour.getValue();
                for (InventoryItem item : items) {
                    int planListingDayOffset = assignDay.getKey() - firstAdjustDayOfWeek;
                    if (planListingDayOffset < 0) {
                        planListingDayOffset += 7;
                    }
                    DateTime planListingDateTime = firstAdjustDay.plusDays(planListingDayOffset);
                    try {
                        InventoryListItem listItem = buildListItem(setting,
                                item,
                                planListingDateTime.withTimeAtStartOfDay(),
                                assignHourTimeInterval
                        );
                        listItems.add(listItem);
                    } catch (InventoryException e) {
                        logger.error("createPlan error, settingId[" + setting.getId() + "]", e);
                    }
                }
            }
        }
        return listItems;
    }

    @Transactional
    private void savePlan(InventoryListSetting setting, List<InventoryListItem> listItems) {
        if (CollectionUtils.isNotEmpty(listItems)) {
            myBatisBatchWriter.write("com.trilemon.boss.inventory.dao.InventoryListItemMapper.insertSelective", listItems);
        }
        InventoryListSetting newSetting = new InventoryListSetting();
        newSetting.setId(setting.getId());
        newSetting.setStatus(SETTING_STATUS_RUNNING);
        newSetting.setLastPlanTime(appService.getLocalSystemTime().toDate());
        inventoryListSettingMapper.updateByPrimaryKeySelective(newSetting);
    }

    public void execPlan(Long settingId) throws TaobaoSessionExpiredException, TaobaoAccessControlException,
            TaobaoEnhancedApiException {
        DateTime now = appService.getLocalSystemTime();
        List<InventoryListItem> plans = inventoryListItemMapper.selectBySettingIdAndStatusAndPlanTime(settingId,
                ImmutableList.of(InventoryConstants.LIST_STATUS_WAITING_ADJUST, InventoryConstants.LIST_STATUS_FAILED),
                now.withTimeAtStartOfDay().toDate(),
                now.toLocalTime().plusHours(1).toDateTimeToday().toDate());
        logger.info("settingId[{}] get [{}] plan to adjust", settingId, plans.size());
        for (InventoryListItem plan : plans) {
            execPlan(plan);
        }
    }

    public void execPlan(InventoryListItem plan) throws TaobaoAccessControlException, TaobaoSessionExpiredException,
            TaobaoEnhancedApiException {
        DateTime now = appService.getLocalSystemTime();
        if (now.toLocalTime().getMillisOfDay() < plan.getPlanAdjustStartTime().getTime()) {
            logger.info("InventoryListItemId[{}] is delay, startTime[{}] but now[{}]", plan.getId(),
                    DateUtils.format(plan.getPlanAdjustEndTime(), DateUtils.yyyy_MM_dd_HH_mm_ss),
                    now.toString(DateUtils.yyyy_MM_dd_HH_mm_ss));
        }

        InventoryListItem planResult = new InventoryListItem();
        planResult.setId(plan.getId());
        try {
            taobaoApiShopService.listItem(plan.getUserId(), plan.getItemNumIid());
            planResult.setFailedCause("");
            planResult.setStatus(InventoryConstants.LIST_STATUS_SUCCESSFUL);
        } catch (BaseTaobaoApiException e) {
            planResult.setStatus(InventoryConstants.LIST_STATUS_FAILED);
            planResult.setFailedCause(ToStringBuilder.reflectionToString(e));
            inventoryListItemMapper.updateByPrimaryKeySelective(planResult);
            throw e;
        }
        inventoryListItemMapper.updateByPrimaryKeySelective(planResult);
    }
}
