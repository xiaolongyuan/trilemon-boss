package com.trilemon.boss.rate.web.controller;

import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.infra.sync.rate.service.RateCalcService;
import com.trilemon.boss.infra.sync.rate.service.RateSyncException;
import com.trilemon.boss.infra.sync.rate.service.RateSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author kevin
 */
@Controller
@RequestMapping("/test")
public class TestController {
    @Autowired
    private RateSyncService rateSyncService;
    @Autowired
    private RateCalcService rateCalcService;

    @ResponseBody
    @RequestMapping(value = "/sync", method = RequestMethod.GET)
    public String rate(@RequestParam Long userId) throws TaobaoEnhancedApiException, TaobaoSessionExpiredException,
            TaobaoAccessControlException, RateSyncException {
        rateSyncService.sync(userId);
        return "success";
    }
    @ResponseBody
    @RequestMapping(value = "/calcSellerDayRate", method = RequestMethod.GET)
    public String calcSellerDayRate(@RequestParam Long userId) throws TaobaoEnhancedApiException, TaobaoSessionExpiredException,
            TaobaoAccessControlException, RateSyncException {
        rateCalcService.calcSellerDayRate(userId);
        return "success";
    }
}
