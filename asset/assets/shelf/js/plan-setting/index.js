define(function(require, exports, module) {
    var NewController = require('./controller/new-controller');
    var FilterController = require('./controller/filter-controller');
    var EditController = require('./controller/edit-controller');
    var IndexController = require('./controller/index-controller');

    var ItemFilter = require('./service/item-filter');
    var PlanSettingForm = require('./service/plan-setting-form');
    var Confirm = require('./service/confirm');

    module.exports = {
        controllers: {
            'planSetting.new': NewController,
            'planSetting.filter': FilterController,
            'planSetting.index': IndexController,
            'planSetting.edit': EditController
        },
        factories: {
            'ItemFilter': ItemFilter,
            'PlanSettingForm': PlanSettingForm,
            'Confirm': Confirm
        }
    };
});
