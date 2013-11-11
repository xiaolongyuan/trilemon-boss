package com.trilemon.boss.showcase.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.taobao.api.domain.Item;
import com.trilemon.boss.showcase.ShowcaseConstants;
import com.trilemon.boss.showcase.ShowcaseException;
import com.trilemon.boss.showcase.dao.AdjustDetailMapper;
import com.trilemon.boss.showcase.dao.SettingMapper;
import com.trilemon.boss.showcase.model.AdjustDetail;
import com.trilemon.boss.showcase.model.Setting;
import com.trilemon.boss.showcase.model.dto.ShowcaseItem;
import com.trilemon.boss360.infrastructure.base.service.AppService;
import com.trilemon.boss360.infrastructure.base.service.api.TaobaoApiShopService;
import com.trilemon.boss360.infrastructure.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss360.infrastructure.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss360.infrastructure.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.commons.Collections3;
import com.trilemon.commons.web.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author kevin
 */
@Service
public class SettingService {
    private final static Logger logger = LoggerFactory.getLogger(SettingService.class);
    @Autowired
    private TaobaoApiShopService taobaoApiShopService;
    @Autowired
    private SettingMapper settingMapper;
    @Autowired
    private AdjustDetailMapper adjustDetailMapper;
    @Autowired
    private AppService appService;
    @Autowired
    private AdjustService adjustService;

    public void createSetting(Long userId, Setting setting) throws ShowcaseException, TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        if (null != settingMapper.selectByUserId(userId)) {
            logger.info("userId[{}] setting exist.", userId);
            return;
        }
        setting.setUserId(userId);
        setting.setStatus(ShowcaseConstants.SETTING_STATUS_RUNNING);
        setting.setAddTime(appService.getLocalSystemTime().toDate());
        settingMapper.insertSelective(setting);
        adjustService.adjust(userId);
    }

    public void updateSetting(Long userId, Setting setting) throws ShowcaseException, TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        setting.setUserId(userId);
        settingMapper.updateByUserIdSelective(setting);
        adjustService.adjust(userId);
    }

    public Setting getSetting(Long userId) throws ShowcaseException {
        return settingMapper.selectByUserId(userId);
    }

    public void resumeSetting(Long userId) throws ShowcaseException, TaobaoSessionExpiredException,
            TaobaoEnhancedApiException, TaobaoAccessControlException {
        settingMapper.updateStatusByUserId(userId, ShowcaseConstants.SETTING_STATUS_RUNNING);
        adjustService.adjust(userId);
    }

    public void pauseSetting(Long userId) throws ShowcaseException, TaobaoSessionExpiredException,
            TaobaoEnhancedApiException {
        settingMapper.updateStatusByUserId(userId, ShowcaseConstants.SETTING_STATUS_PAUSE);
    }

    /**
     * 增加必推商品
     *
     * @param userId
     * @param numIid
     * @return
     * @throws ShowcaseException
     */
    public boolean addIncludeItem(Long userId, Long numIid) throws ShowcaseException {
        return includeItem(userId, numIid, true);
    }

    /**
     * 删除必推商品
     *
     * @param userId
     * @param numIid
     * @return
     * @throws ShowcaseException
     */
    public boolean deleteIncludeItem(Long userId, Long numIid) throws ShowcaseException {
        return includeItem(userId, numIid, false);
    }

    /**
     * 增加不推商品
     *
     * @param userId
     * @param numIid
     * @return
     * @throws ShowcaseException
     */
    public boolean addExcludeItem(Long userId, Long numIid) throws ShowcaseException {
        return excludeItem(userId, numIid, true);
    }

    /**
     * 删除不推商品
     *
     * @param userId
     * @param numIid
     * @return
     * @throws ShowcaseException
     */
    public boolean deleteExcludeItem(Long userId, Long numIid) throws ShowcaseException {
        return excludeItem(userId, numIid, false);
    }

    /**
     * 必推商品
     *
     * @param userId
     * @param numIid * @param addOrDelete true is add,false is delete
     * @return
     */
    public boolean includeItem(Long userId, Long numIid, boolean addOrDelete) throws ShowcaseException {
        Setting setting = settingMapper.selectByUserId(userId);
        final List<Long> includeNumIids = Collections3.getLongList(setting.getIncludeItemNumIids());
        final List<Long> excludeNumIids = Collections3.getLongList(setting.getExcludeItemNumIids());
        if (addOrDelete) {
            if (excludeNumIids.contains(numIid)) {
                throw new ShowcaseException("userId[" + userId + "] can not add include item[" + numIid + "], " +
                        "it already in exclude items.");
            }
            includeNumIids.add(numIid);
        } else {
            includeNumIids.remove(numIid);
        }
        Setting updateSetting = new Setting();
        updateSetting.setId(setting.getId());
        updateSetting.setIncludeItemNumIids(Collections3.COMMA_JOINER.join(includeNumIids));
        int rows = settingMapper.updateByPrimaryKeySelective(updateSetting);
        return rows == 1;
    }

    /**
     * 不必推商品
     *
     * @param userId
     * @param numIid
     * @param addOrDelete true is add,false is delete
     * @return
     */
    public boolean excludeItem(Long userId, Long numIid, boolean addOrDelete) throws ShowcaseException {
        Setting setting = settingMapper.selectByUserId(userId);
        final List<Long> includeNumIids = Collections3.getLongList(setting.getIncludeItemNumIids());
        final List<Long> excludeNumIids = Collections3.getLongList(setting.getExcludeItemNumIids());
        if (addOrDelete) {
            if (includeNumIids.contains(numIid)) {
                throw new ShowcaseException("userId[" + userId + "] can not add include item[" + numIid + "], it already in exclude items.");
            }
            excludeNumIids.add(numIid);
        } else {
            excludeNumIids.remove(numIid);
        }
        Setting updateSetting = new Setting();
        updateSetting.setId(setting.getId());
        updateSetting.setExcludeItemNumIids(Collections3.COMMA_JOINER.join(excludeNumIids));
        int rows = settingMapper.updateByPrimaryKeySelective(updateSetting);
        return rows == 1;
    }

    public Page<ShowcaseItem> paginateShowcaseItems(Long userId, String query, int pageNum,
                                                    int pageSize) throws TaobaoEnhancedApiException, TaobaoSessionExpiredException, TaobaoAccessControlException {
        checkNotNull(userId, "userId must be not null.");

        int totalSize = adjustDetailMapper.countByUserIdAndQuery(userId, query);
        List<AdjustDetail> adjustDetails = adjustDetailMapper.paginateByUserIdAndQuery(userId,
                query, (pageNum - 1) * pageSize,pageSize);
        if (CollectionUtils.isEmpty(adjustDetails)) {
            return Page.create(totalSize, pageNum, pageSize, Lists.<ShowcaseItem>newArrayList());
        } else {
            Setting setting = settingMapper.selectByUserId(userId);
            final List<Long> excludeNumIids = Collections3.getLongList(setting.getExcludeItemNumIids());
            List<ShowcaseItem> showcaseItems = Lists.transform(adjustDetails, new Function<AdjustDetail, ShowcaseItem>() {
                @Nullable
                @Override
                public ShowcaseItem apply(@Nullable AdjustDetail input) {
                    ShowcaseItem showcaseItem = new ShowcaseItem();
                    Item item = new Item();
                    item.setNumIid(input.getItemNumIid());
                    item.setTitle(input.getItemTitle());
                    item.setPicUrl(input.getItemPicUrl());
                    showcaseItem.setItem(item);
                    if (excludeNumIids.contains(input.getItemNumIid())) {
                        showcaseItem.setStatus(ShowcaseConstants.ITEM_EXCLUDE);
                    } else {
                        showcaseItem.setStatus(ShowcaseConstants.ITEM_SHOWCASE);
                    }
                    return showcaseItem;
                }
            });
            return Page.create(totalSize, pageNum, pageSize, showcaseItems);
        }
    }

    /**
     * 查询库存商品
     *
     * @param userId
     * @param query
     * @param banners      库存类型
     * @param sellerCatIds
     * @param pageNum
     * @param pageSize
     * @param fuzzy
     * @return
     * @throws TaobaoEnhancedApiException
     * @throws TaobaoSessionExpiredException
     * @throws TaobaoAccessControlException
     */
    public Page<ShowcaseItem> paginateInventoryItems(Long userId, String query,
                                                     List<String> banners,
                                                     List<String> sellerCatIds,
                                                     long pageNum,
                                                     long pageSize,
                                                     boolean fuzzy,
                                                     String order) throws TaobaoEnhancedApiException,
            TaobaoSessionExpiredException, TaobaoAccessControlException {
        Page<Item> itemPage = taobaoApiShopService.paginateInventoryItems(userId,
                query,
                ShowcaseConstants.ITEM_FIELDS,
                banners,
                null == sellerCatIds ? null : Collections3.stringList2LongList(sellerCatIds),
                pageNum,
                pageSize,
                fuzzy,
                order);
        Setting setting = settingMapper.selectByUserId(userId);
        final List<Long> includeNumIids = Collections3.getLongList(setting.getIncludeItemNumIids());
        List<ShowcaseItem> showcaseItems = Lists.transform(itemPage.getItems(), new Function<Item, ShowcaseItem>() {
            @Nullable
            @Override
            public ShowcaseItem apply(@Nullable Item input) {
                ShowcaseItem showcaseItem = new ShowcaseItem();
                showcaseItem.setItem(input);
                if (includeNumIids.contains(input.getNumIid())) {
                    showcaseItem.setStatus(ShowcaseConstants.ITEM_INCLUDE);
                } else {
                    showcaseItem.setStatus(ShowcaseConstants.ITEM_SHOWCASE);
                }
                return showcaseItem;
            }
        });
        Page<ShowcaseItem> showcaseItemPage = Page.create(itemPage.getTotalSize(), itemPage.getPageNum(),
                itemPage.getPageSize(), showcaseItems);
        return showcaseItemPage;
    }

    /**
     * 查询在售商品
     *
     *
     * @param userId
     * @param query
     * @param sellerCatIds
     * @param pageNum
     * @param pageSize
     * @param fuzzy
     * @param order
     * @return
     * @throws TaobaoEnhancedApiException
     * @throws TaobaoSessionExpiredException
     * @throws TaobaoAccessControlException
     */
    public Page<ShowcaseItem> paginateOnSaleItems(Long userId, String query,
                                                  List<String> sellerCatIds,
                                                  long pageNum,
                                                  long pageSize,
                                                  boolean fuzzy,
                                                  String order) throws TaobaoEnhancedApiException,
            TaobaoSessionExpiredException, TaobaoAccessControlException {
        Page<Item> itemPage = taobaoApiShopService.paginateOnSaleItems(userId,
                query,
                ShowcaseConstants.ITEM_FIELDS,
                null == sellerCatIds ? null : Collections3.stringList2LongList(sellerCatIds),
                pageNum,
                pageSize,
                fuzzy,
                null,
                order);
        Setting setting = settingMapper.selectByUserId(userId);
        final List<Long> includeNumIids = Collections3.getLongList(setting.getIncludeItemNumIids());
        List<ShowcaseItem> showcaseItems = Lists.transform(itemPage.getItems(), new Function<Item, ShowcaseItem>() {
            @Nullable
            @Override
            public ShowcaseItem apply(@Nullable Item input) {
                ShowcaseItem showcaseItem = new ShowcaseItem();
                showcaseItem.setItem(input);
                if (includeNumIids.contains(input.getNumIid())) {
                    showcaseItem.setStatus(ShowcaseConstants.ITEM_INCLUDE);
                } else {
                    showcaseItem.setStatus(ShowcaseConstants.ITEM_SHOWCASE);
                }
                return showcaseItem;
            }
        });
        Page<ShowcaseItem> showcaseItemPage = Page.create(itemPage.getTotalSize(), itemPage.getPageNum(),
                itemPage.getPageSize(), showcaseItems);
        return showcaseItemPage;
    }
}