(function () {
    'use strict';

    angular.module('JahiaOAuthApp').controller('SamlController', SamlController);

    SamlController.$inject = ['$location', 'settingsService', 'helperService', 'i18nService'];

    function SamlController($location, settingsService, helperService, i18nService) {
        var vm = this;

        // Variables
        vm.expandedCard = false;
        vm.callbackUrls = [];
        vm.callbackUrl = '';

        // Functions
        vm.saveSettings = saveSettings;
        vm.goToMappers = goToMappers;
        vm.toggleCard = toggleCard;
        vm.fileNameChanged = fileNameChanged;
        vm.metadata = metadata;

        vm.availableKeyStoreTypes = {
            "JKS": "JKS",
            "JCEKS": "JCEKS",
            "PKCS12": "PKCS12",
            "PKCS12S2": "PKCS12S2"
        }
        vm.availableBindings = {
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST": "POST",
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect": "Redirect",
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact": "Artifact",
            "urn:oasis:names:tc:SAML:2.0:bindings:SOAP": "SOAP",
            "urn:oasis:names:tc:SAML:2.0:bindings:PAOS": "PAOS",
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign": "POST-SimpleSign"
        }

        init();

        function saveSettings() {
            // Value can't be empty
            const error = validate();
            if (error) {
                helperService.errorToast(error);
                return false;
            }


            // the node name here must be the same as the one in your spring file
            settingsService.setConnectorData({
                connectorServiceName: 'Saml',
                file_identityProviderMetadata: vm.identityProviderMetadata,
                file_keyStore: vm.keyStore,
                properties: {
                    enabled: vm.enabled,
                    identityProviderMetadata: vm.identityProviderMetadata,
                    keyStore: vm.keyStore,
                    relyingPartyIdentifier: vm.relyingPartyIdentifier,
                    keyStoreType: vm.keyStoreType,
                    keyStoreAlias: vm.keyStoreAlias,
                    keyStorePass: vm.keyStorePass,
                    privateKeyPass: vm.privateKeyPass,
                    incomingTargetUrl: vm.incomingTargetUrl,
                    postLoginPath: vm.postLoginPath,
                    maximumAuthenticationLifetime: vm.maximumAuthenticationLifetime,
                    forceAuth: vm.forceAuth,
                    passive: vm.passive,
                    signAuthnRequest: vm.signAuthnRequest,
                    requireSignedAssertions: vm.requireSignedAssertions,
                    bindingType: vm.bindingType,
                }
            }).success(function () {
                vm.connectorHasSettings = true;
                helperService.successToast(i18nService.message('angular.saml2.directives.settings.ma-settings.message.settingsSaved'));
            }).error(function (data) {
                helperService.errorToast(i18nService.message('angular.saml2.directives.settings.ma-settings.message.error') + ' ' + data.error);
                console.log(data);
            });
        }

        function goToMappers() {
            // the second part of the path must be the service name
            $location.path('/mappers/Saml');
        }

        function toggleCard() {
            vm.expandedCard = !vm.expandedCard;
        }

        function fileNameChanged(element) {
            vm[element.getAttribute('name')] = element.files[0];
        }


        function metadata() {
            window.open(maContextInfos.siteKey + ".saml2Metadata.do")
        }

        function init() {
            i18nService.addKey(saml2i18n);

            settingsService.getConnectorData('Saml', ['enabled', 'relyingPartyIdentifier', 'keyStoreType', 'keyStoreAlias', 'keyStorePass', 'privateKeyPass', 'incomingTargetUrl', 'postLoginPath', 'maximumAuthenticationLifetime', 'forceAuth', 'passive', 'signAuthnRequest', 'requireSignedAssertions', 'bindingType']).success(function (data) {
                if (data && !angular.equals(data, {})) {
                    vm.connectorHasSettings = true;
                    vm.enabled = data.enabled;
                    vm.relyingPartyIdentifier = data.relyingPartyIdentifier;
                    vm.keyStoreType = data.keyStoreType;
                    vm.keyStoreAlias = data.keyStoreAlias;
                    vm.keyStorePass = data.keyStorePass;
                    vm.privateKeyPass = data.privateKeyPass;
                    vm.incomingTargetUrl = data.incomingTargetUrl;
                    vm.postLoginPath = data.postLoginPath;
                    vm.maximumAuthenticationLifetime = data.maximumAuthenticationLifetime;
                    vm.forceAuth = data.forceAuth;
                    vm.passive = data.passive;
                    vm.signAuthnRequest = data.signAuthnRequest;
                    vm.requireSignedAssertions = data.requireSignedAssertions;
                    vm.bindingType = data.bindingType;
                } else {
                    vm.connectorHasSettings = false;
                    vm.enabled = false;
                }
            }).error(function (data) {
                helperService.errorToast(i18nService.message('joant_samlOAuthView.message.label') + ' ' + data.error);
            });
        }

        function validate() {
            if (vm.enabled) {
                if (!vm.relyingPartyIdentifier) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.relyingPartyIdentifier');
                }

                if (!vm.incomingTargetUrl) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.incomingTargetUrl');
                }

                if (!vm.keyStorePass) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.keyStorePass');
                }

                if (!vm.privateKeyPass) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.privateKeyPass');
                }

                if (!vm.postLoginPath) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.postLoginPath');
                }

                if (!vm.maximumAuthenticationLifetime) {
                    return i18nService.message('angular.saml2.directives.settings.ma-settings.validate.message.maximumAuthenticationLifetime');
                }
            }

            return false;
        };

    }
})();
