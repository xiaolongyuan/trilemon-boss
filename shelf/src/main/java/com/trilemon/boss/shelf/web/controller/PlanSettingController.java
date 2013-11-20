package com.trilemon.boss.shelf.web.controller;

import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.shelf.ShelfException;
import com.trilemon.boss.shelf.ShelfUtils;
import com.trilemon.boss.shelf.model.Plan;
import com.trilemon.boss.shelf.model.PlanSetting;
import com.trilemon.boss.shelf.model.dto.ShelfItem;
import com.trilemon.boss.shelf.service.PlanService;
import com.trilemon.boss.shelf.service.PlanSettingService;
import com.trilemon.commons.web.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author kevin
 */
@Controller
@RequestMapping("/plan-settings")
public class PlanSettingController {
    @Autowired
    AppService appService;
    @Autowired
    private PlanSettingService planSettingService;
    @Autowired
    private PlanService planService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public Page<PlanSetting> index(@RequestParam(defaultValue = "1") int page) {
        return planSettingService.paginatePlanSettings(56912708L, page, 2);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public Object create(@RequestBody @Valid PlanSetting planSetting, BindingResult result, HttpServletResponse response) throws ShelfException, TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        if (result.hasErrors()) {
            response.setStatus(422);
            return result.getAllErrors();
        } else {
            planSettingService.createPlanSetting(56912708L, planSetting);
            return planSetting;
        }
    }

    @RequestMapping(value = "/{planSettingId}", method = RequestMethod.GET)
    @ResponseBody
    public PlanSetting show(@PathVariable Long planSettingId) {
        return planSettingService.getPlanSetting(56912708L, planSettingId);
    }

    @RequestMapping(value = "/{planSettingId}", method = RequestMethod.PUT)
    @ResponseBody
    public Object update(@PathVariable Long planSettingId, @RequestBody @Valid PlanSetting planSetting, BindingResult result) throws TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        try {
            planSetting.setId(planSettingId);
            planSettingService.updatePlanSetting(56912708L, planSetting);
            return planSetting;
        } catch (ShelfException e) {
            return e;
        }
    }

    /**
     * 删除计划
     *
     * @param planSettingId
     * @return
     */
    @RequestMapping(value = "/{planSettingId}", method = RequestMethod.DELETE)
    @ResponseBody
    public boolean delete(@PathVariable Long planSettingId) {
        return planSettingService.deletePlanSetting(56912708L, planSettingId);
    }

    /**
     * 暂停计划
     *
     * @param planSettingId
     * @return
     */
    @RequestMapping(value = "/{planSettingId}/pause", method = RequestMethod.POST)
    @ResponseBody
    public PlanSetting pause(@PathVariable Long planSettingId) {
        planSettingService.pausePlanSetting(56912708L, planSettingId);
        return planSettingService.getPlanSetting(56912708L, planSettingId);
    }

    /**
     * 继续计划
     *
     * @param planSettingId
     * @return
     */
    @RequestMapping(value = "/{planSettingId}/pause", method = RequestMethod.DELETE)
    @ResponseBody
    public PlanSetting resume(@PathVariable Long planSettingId) {
        planSettingService.resumePlanSetting(56912708L, planSettingId);
        return planSettingService.getPlanSetting(56912708L, planSettingId);
    }

    /**
     * 计划对应的宝贝
     *
     * @param key
     * @param page
     * @param planSettingId
     * @return
     * @throws TaobaoEnhancedApiException
     * @throws TaobaoSessionExpiredException
     */
    @ResponseBody
    @RequestMapping(value = "/{planSettingId}/items", method = RequestMethod.GET)
    public Page<ShelfItem> indexItem(String key,
                                     @RequestParam(defaultValue = "1") int page,
                                     @PathVariable Long planSettingId) throws TaobaoEnhancedApiException,
            TaobaoSessionExpiredException {
        Page<Plan> plans = planSettingService.paginatePlans(56912708L, planSettingId, key, page, 2);
        Page<ShelfItem> itemPage = Page.empty();
        itemPage.setTotalSize(plans.getTotalSize());
        itemPage.setPageSize(plans.getPageSize());
        itemPage.setPageNum(plans.getPageNum());
        itemPage.setItems(ShelfUtils.planToItem(plans.getItems()));
        return itemPage;
    }

    /**
     * 排除宝贝
     *
     * @param numIid
     * @return
     */
    @RequestMapping(value = "/{planSettingId}/items/{numIid}/exclude", method = RequestMethod.POST)
    @ResponseBody
    public ShelfItem excludeItem(@PathVariable Long planSettingId, @PathVariable Long numIid) {
        planService.excludeItem(56912708L, planSettingId, numIid);
        Plan plan = planSettingService.getPlan(56912708L, numIid);
        return ShelfUtils.planToItem(plan);
    }

    /**
     * 取消排除宝贝
     *
     * @param numIid
     * @return
     */
    @RequestMapping(value = "/{planSettingId}/items/{numIid}/exclude", method = RequestMethod.DELETE)
    @ResponseBody
    public ShelfItem includeItem(@PathVariable Long planSettingId, @PathVariable Long numIid) {
        planService.includeItem(56912708L, planSettingId, numIid);
        Plan plan = planSettingService.getPlan(56912708L, numIid);
        return ShelfUtils.planToItem(plan);
    }

    /**
     * 获取用于展示店铺上下架图形的数据
     *
     * @return
     * @throws ShelfException
     */
    @ResponseBody
    @RequestMapping(value = "/chart", method = RequestMethod.GET)
    public List<Integer> chart() throws
            ShelfException, TaobaoEnhancedApiException, TaobaoSessionExpiredException, TaobaoAccessControlException {
        return planSettingService.getShelfStatus(56912708L).getListItemNum();
    }

    /**
     * 获取计划的上架时间安排，用于调整时间
     * @param planSettingId
     * @param resp
     * @throws IOException
     */
    @RequestMapping(value = "/{planSettingId}/distribution", method = RequestMethod.GET)
    public void getDistribution(@PathVariable Long planSettingId, HttpServletResponse resp) throws IOException {
        PlanSetting planSetting = planSettingService.getPlanSetting(56912708L, planSettingId);
        resp.setContentType("application/json");
        resp.getWriter().write(planSetting.getDistribution());
    }

    /**
     * 修改计划的上架时间安排
     * @param planSettingId
     * @param distribution
     * @throws TaobaoSessionExpiredException
     * @throws ShelfException
     * @throws TaobaoEnhancedApiException
     * @throws TaobaoAccessControlException
     */
    @ResponseBody
    @RequestMapping(value = "/{planSettingId}/distribution", method = RequestMethod.PUT)
    public void updateDistribution(@PathVariable Long planSettingId, @RequestBody Map<String, Map<String, Boolean>> distribution) throws TaobaoSessionExpiredException, ShelfException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        planSettingService.updatePlanSettingDistribution(56912708L, planSettingId, distribution);
    }
}
