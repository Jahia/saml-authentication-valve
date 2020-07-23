angular.module('jahia.saml2')
    .directive('maSettings', ['maContextInfos', function (maContextInfos) {
        return {
            templateUrl: maContextInfos.moduleBase + "/javascript/saml2/directives/settings/ma-settings.html",
            controller: ['$scope', 'maSettingsService', '$mdToast', '$mdDialog', 'i18nService',
                function ($scope, maSettingsService, $mdToast, $mdDialog, i18nService) {
                    maSettingsService.getSettings().error(function () {
                        // console.log("saml2: no settings yet")
                    }).success(function (settingsData) {
                        // console.log("getSettings: ", settingsData);
                        if (settingsData) {
                            $scope.settings = settingsData;
                            // console.log("saml2: settings loaded");
                        }
                    });

                    $scope.fileNameChanged = function (element) {
                        $scope.settings[element.getAttribute('name')] = element.files[0];
                    }

                    $scope.save = function () {
                        var error = $scope.validate();

                        if (error) {
                            $scope._displayMsg(true, error);
                            return;
                        }

                        maSettingsService.saveSettings($scope.settings)
                            .success(function (settingsData) {
                                $scope.settings = settingsData;
                                $scope._displayMsg(false, i18nService.message('angular.saml2.directives.settings.ma-settings.message.settingsSaved'));
                            }).error(function (error) {
                            if (error && error.type) {
                                if (error.error) {
                                    $scope._displayMsg(true, error.error);
                                }
                            }
                        });
                    };

                    $scope.enable = function () {
                        console.log("enabled", $scope.settings.enabled);
                    };

                    $scope._displayMsg = function (isError, msg) {
                        var toast = $mdToast.simple()
                            .content(msg)
                            .position("bottom right")
                            .hideDelay(5000);
                        if (isError) {
                            toast.theme("alert");
                        }
                        $mdToast.show(toast);
                    };

                    $scope.metadata = function () {
                        window.open(maContextInfos.siteKey + ".saml2Metadata.do")
                    }


                    $scope.validate = function () {

                        if ($scope.settings.enabled) {
                            if (!$scope.settings.relyingPartyIdentifier) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.relyingPartyIdentifier');
                            }

                            if (!$scope.settings.incomingTargetUrl) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.incomingTargetUrl');
                            }

                            if (!$scope.settings.keyStorePass) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.keyStorePass');
                            }

                            if (!$scope.settings.privateKeyPass) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.privateKeyPass');
                            }
                            if (!$scope.settings.postLoginPath) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.postLoginPath');
                            }
                            if (!$scope.settings.maximumAuthenticationLifetime) {
                                return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.maximumAuthenticationLifetime');
                            }
                        }

                        return false;
                    };

                    $scope.cardContentHeight = function () {
                        var height = window.innerHeight - 250;
                        return "height:" + height + "px;min-height: 400px;"
                    }
                }]
        }
    }]);
