package com.trilemon.boss.showcase.web.controller;

import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.showcase.ShowcaseException;
import com.trilemon.boss.showcase.model.dto.ShowcaseItem;
import com.trilemon.boss.showcase.service.SettingService;
import com.trilemon.commons.web.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 规则对应的宝贝，也就是分类对应的
 *
 * @author edokeh
 */
@Controller
@RequestMapping("/setting-items")
public class SettingItemController {
    @Autowired
    private SettingService settingService;


    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public Page<ShowcaseItem> index(String key, @RequestParam(defaultValue = "1") Integer page, String category) throws
            ShowcaseException,
            TaobaoSessionExpiredException,
            TaobaoEnhancedApiException, TaobaoAccessControlException {
        // 库存中还是销售中
        if ("inventory".equals(category)) {
            return settingService.paginateInventoryGeneralRuleItems(56912708L, key, page, 2);
        } else {
            return settingService.paginateOnSaleGeneralRuleItems(56912708L, key, page, 2);
        }
    }

    /**
     * 排除宝贝
     *
     * @param numIid
     * @return
     */
    @RequestMapping(value = "/exclude", method = RequestMethod.POST)
    @ResponseBody
    public String excludeItem(@RequestBody ExcludeJSONParam excludeParam) throws ShowcaseException, TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        for (Long numIid : excludeParam.numIids) {
            settingService.addExcludeItem(56912708L, numIid);
        }
        return "";
    }

    /**
     * 取消排除宝贝
     *
     * @param numIid
     * @return
     */
    @RequestMapping(value = "/exclude", method = RequestMethod.DELETE)
    @ResponseBody
    public String includeItem(@RequestParam Long[] numIids) throws ShowcaseException, TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        for (Long numIid : numIids) {
            settingService.deleteExcludeItem(56912708L, numIid);
        }
        return "";
    }

    /**
     * 辅助用于接收 JSON Request
     */
    public static class ExcludeJSONParam{
        public Long[] numIids;
    }
}
