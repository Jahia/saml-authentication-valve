sangular.module('JahiaOAuthApp', ['ngMaterial', 'ngSanitize', 'ngRoute'])
    .config(function ($mdThemingProvider) {
        // theme used to create error toast
        $mdThemingProvider.theme('alert');
    })
    .filter('selectable', selectableFilter)
    .filter('typeMatch', typeMatchFilter);

function typeMatchFilter() {
    return function (options, type) {
        var newOptions = [];
        angular.forEach(options, function(option) {
            if (option.valueType == type) {
                newOptions.push(option);
            }
        });
        return newOptions;
    }
}

function selectableFilter() {
    function isNotMappedOrCurrentlySelected(fieldName, params) {
        var isNotMappedOrCurrentlySelected = true;
        if (params.selected && params.selected.name == fieldName) {
            return isNotMappedOrCurrentlySelected;
        } else {
            angular.forEach(params.mapping, function (entry) {
                if (entry[params.key] && entry[params.key].name == fieldName) {
                    isNotMappedOrCurrentlySelected = false;
                }
            });
        }
        return isNotMappedOrCurrentlySelected;
    }

    return function (options, params) {
        if (options.length == 0 || params.mapping.length == 0) {
            return options;
        } else {
            var newOptions = [];
            angular.forEach(options, function (option) {
                if (isNotMappedOrCurrentlySelected(option.name, params)) {
                    newOptions.push(option);
                }
            });
            return newOptions;
        }
    }
}

