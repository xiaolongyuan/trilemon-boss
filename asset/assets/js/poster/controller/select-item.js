/**
 * 选择宝贝
 */
define(function(require, exports, module) {

    var Controller = ['$scope', 'PosterItem', 'PosterTemplate', '$routeParams', '$location', 'Flash', 'PosterSellerCat', 'PosterActivity', function($scope, PosterItem, PosterTemplate, $routeParams, $location, Flash, PosterSellerCat, PosterActivity) {

        // 初始化
        $scope.init = function() {
            if (!$routeParams.templateId && !$routeParams.activityId) {
                $location.url('/poster/category');
                return;
            }
            // 修改或创建
            if ($routeParams.activityId) {
                $scope.activity = PosterActivity.get({id: $routeParams.activityId, detail: true});
                $scope.template = $scope.activity.template;
            }
            else {
                $scope.activity = new PosterActivity({
                    templateId: $routeParams.templateId,
                    activityItems: []
                });
                $scope.template = PosterTemplate.get({id: $routeParams.templateId});
            }
            $scope.searchKey = $routeParams.key;
            getItems();
            $scope.sellerCats = PosterSellerCat.query();

        };

        $scope.init();

        // 加入或取消
        $scope.setInclude = function(item, flag) {
            item.include = flag;
            if (flag) {
                $scope.activity.activityItems.push(item);
            }
            else {
                $scope.activity.activityItems = _.filter($scope.activity.activityItems, function(i) {
                    return i.numIid != item.numIid;
                });
            }
        };

        // 下一步，保存结果
        $scope.save = function() {
            PosterActivity.save(_.pick($scope.activity, 'id', 'activityItems', 'templateId'), function(data) {
                $location.url('/poster/activity/' + data.id + '/preview');
            });
        };

        // 切换分类
        $scope.changeCategory = function() {
            $scope.searchKey = '';
            getItems({category: $scope.category, key: '', page: 1});
        };

        // 搜索
        $scope.search = function() {
            getItems({key: $scope.searchKey, page: 1});
        };

        $scope.jumpPage = function(page) {
            getItems({page: page});
        };

        // 获取宝贝列表，可以传入分类、页码等
        function getItems(options) {
            // 合并 URL 上的参数，并将新参数再次写入 URL
            options = _.defaults(options || {}, $routeParams);
            $location.search(options);

            $scope.items = PosterItem.query(_.omit(options, 'templateId', 'activityId'), function(data) {
                // 选中的回填
                var ids = _.pluck($scope.activity.activityItems, 'numIid');
                _.each(data, function(item) {
                    item.include = _.contains(ids, item.numIid);
                });
            });
            return $scope.items.$promise;
        }
    }];

    Controller.title = '选择宝贝 - 宝贝海报';
    Controller.template = 'poster/selectItem';
    Controller.navClass = 'posterSetting';

    module.exports = Controller;
});