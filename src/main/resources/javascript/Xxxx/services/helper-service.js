(function() {
    'use strict';

    angular.module('JahiaOAuthApp').service('helperService', helperService);

    helperService.$inject = ['$mdToast'];

    function helperService($mdToast) {
        return {
            successToast: successToast,
            errorToast: errorToast
        };

        function successToast(message) {
            $mdToast.show(
                $mdToast.simple()
                    .textContent(message)
                    .theme('success-toast')
                    .position('bottom right')
            );
        }

        function errorToast(message) {
            $mdToast.show(
                $mdToast.simple()
                    .textContent(message)
                    .theme('error-toast')
                    .position('bottom right')
            );
        }
    }
})();