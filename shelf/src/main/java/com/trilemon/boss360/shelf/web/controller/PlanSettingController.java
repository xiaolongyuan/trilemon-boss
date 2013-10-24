package com.trilemon.boss360.shelf.web.controller;

import com.trilemon.boss360.infrastructure.base.service.AppService;
import com.trilemon.boss360.shelf.ShelfConstants;
import com.trilemon.boss360.shelf.ShelfException;
import com.trilemon.boss360.shelf.model.PlanSetting;
import com.trilemon.boss360.shelf.service.PlanSettingService;
import com.trilemon.commons.web.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public Page<PlanSetting> index(@RequestParam(defaultValue = "1") int page) {
        return planSettingService.paginatePlanSettings(56912708L, page, 5);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public Object create(@RequestBody @Valid PlanSetting planSetting, BindingResult result, HttpServletResponse response) throws ShelfException {
        if (result.hasErrors()) {
            response.setStatus(422);
            return result.getAllErrors();
        } else {
            planSetting.setAddTime(appService.getLocalSystemTime().toDate());
            planSetting.setDistribution("test");
            planSetting.setDistributionType(ShelfConstants.PLAN_SETTING_DISTRIBUTE_TYPE_AUTO);
            //planSetting.setExcludeItemIids("19491833743,19440841598");
            planSetting.setStatus(ShelfConstants.PLAN_SETTING_STATUS_WAITING_PLAN);
            planSetting.setUserId(56912708L);
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
    public Object update(@PathVariable Long planSettingId, @RequestBody @Valid PlanSetting planSetting, BindingResult result) {
        try {
            planSetting.setId(planSettingId);
            planSettingService.updatePlanSetting(56912708L, planSetting);
            return planSetting;
        } catch (ShelfException e) {
            return e;
        }
    }

    @RequestMapping(value = "/{planSettingId}", method = RequestMethod.DELETE)
    @ResponseBody
    public boolean delete(@PathVariable Long planSettingId) {
        return planSettingService.deletePlanSetting(56912708L, planSettingId);
    }
}