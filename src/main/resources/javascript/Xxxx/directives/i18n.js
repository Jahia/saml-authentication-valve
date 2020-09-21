angular.module('JahiaOAuthApp')
    .service('i18nService', ["maContextInfos", function (maContextInfos) {
        this.message = function (key) {
            if (maContextInfos.i18nLabels && maContextInfos.i18nLabels[key]) {
                return maContextInfos.i18nLabels[key];
            } else {
                return "???" + key + "???";
            }
        };

        this.format = function (key, params) {
            var replacer = function(params){
                return function(s, index) {
                    return params[index] ? (params[index] == '__void__' ? "" : params[index]) : "";
                };
            };

            if(params){
                if (maContextInfos.i18nLabels && maContextInfos.i18nLabels[key]) {
                    return maContextInfos.i18nLabels[key].replace(/\{(\w+)\}/g, replacer(params.split('|')));
                } else {
                    return "???" + key + "???";
                }
            } else {
                return this.message(key);
            }
        };

        this.addKey = function (newI18nMap) {
            angular.forEach(newI18nMap, function (value, key) {
                maContextInfos.i18nLabels[key] = value;
            });
        }

    }])

    .directive("messageKey", function (i18nService) {
        return {
            restrict: 'A',
            link: function ($scope, $element, $attrs) {
                var i18n;
                if(!$attrs.messageParams){
                    i18n = i18nService.message($attrs.messageKey);
                } else {
                    i18n = i18nService.format($attrs.messageKey, $attrs.messageParams);
                }

                if($attrs.messageAttr) {
                    // store the i18n in the specified element attr
                    $element.attr($attrs.messageAttr, i18n);
                } else {
                    // set the i18n as element text
                    $element.text(i18n);
                }
            }
        };
    })

    .filter('translate', function(i18nService) {
        return function(input) {
            return i18nService.message(input);
        };
    });
