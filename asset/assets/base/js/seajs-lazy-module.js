angular.module('seajs', []).config(['$controllerProvider', '$compileProvider', '$filterProvider', '$provide', function($controllerProvider, $compileProvider, $filterProvider, $provide) {
    var register = {
        controller: $controllerProvider.register,
        directive: $compileProvider.directive,
        filter: $filterProvider.register,
        factory: $provide.factory,
        service: $provide.service,
        decorator: $provide.decorator
    };


    $provide.provider('SeajsLazyModule', SeajsLazyModuleProvider);

    function SeajsLazyModuleProvider() {
        this.$get = ['$rootScope', function($rootScope) {
            return new SeajsLazyModule($rootScope, register);
        }];

        // 快捷方法，用于创建一个 module 的配置
        this.create = function(moduleUrl) {
            return {
                routeFor: function(controllerName) {
                    return {
                        moduleUrl: moduleUrl,
                        controller: controllerName
                    };
                }
            };
        };

        this.setTilteSuffix = function(suffix) {
            SeajsLazyModule.titleSuffix = suffix;
        };
    }

    function SeajsLazyModule($rootScope, register) {
        this.$rootScope = $rootScope;
        this.register = register;
        this.modules = {};
    }

    SeajsLazyModule.prototype = {
        constructor: SeajsLazyModule,

        // 初始化
        init: function() {
            var _this = this;
            this.$rootScope.$on('$routeChangeStart', function(e, target) {
                var route = target && target.$$route;
                if (route) {
                    route.resolve = route.resolve || {};
                    _this.handleRouteChange(route);
                }
            });
        },

        // 处理 URL 变化，为现有的 route 对象增加 resolve
        handleRouteChange: function(route) {
            var module = this.modules[route.moduleUrl];
            // 如已有缓存
            if (module) {
                route.resolve.module = function() {
                    return module;
                };
                route.resolve.$template = function() {
                    return module.controllers[route.controller].template;
                };
                this.resolveModule(module, route.controller);
            }
            // 异步加载
            else {
                var _this = this;

                route.resolve.module = ['$q', function($q) {
                    var defer = $q.defer();
                    seajs.use(route.moduleUrl, function(m) {
                        _this.registerModule(route, m);
                        if (_this.tplDefer.tag === route.moduleUrl + route.controller) {
                            _this.tplDefer.resolve(m.controllers[route.controller].template);
                            _this.resolveModule(m, route.controller);
                            defer.resolve(m);
                        }
                    });
                    return defer.promise;
                }];
                route.resolve.$template = ['$q', function($q) {
                    _this.tplDefer = $q.defer();
                    _this.tplDefer.tag = route.moduleUrl + route.controller;
                    return _this.tplDefer.promise;
                }];
            }
        },

        // 注册 module，缓存、并将 controller/service 等注册
        registerModule: function(route, module) {
            this.modules[route.moduleUrl] = module;

            this.register.controller(module.controllers);
            this.register.factory(module.factories);
        },

        resolveModule: function(module, controller) {
            this.$rootScope.title = module.controllers[controller].title + (SeajsLazyModule.titleSuffix || '');
            this.$rootScope.navClass = module.controllers[controller].navClass;
        }
    };
}]);