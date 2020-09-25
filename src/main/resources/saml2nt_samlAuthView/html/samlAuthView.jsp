<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="i18n/saml-authentication-valve-i18n_${currentResource.locale}.js"
                       var="i18nJSFile"/>
<c:if test="${empty i18nJSFile}">
    <template:addResources type="javascript" resources="i18n/saml-authentication-valve-i18n.js"/>
</c:if>
<template:addResources type="javascript" resources="saml/saml-controller.js"/>

<md-card ng-controller="SamlController as saml">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline"
                      message-key="angular.saml2.directives.settings.ma-settings.subtitle.saml2Setting"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="saml.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="angular.saml2.directives.settings.ma-settings.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!saml.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="saml.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="saml.expandedCard">

        <form name="samlForm">

            <md-switch ng-model="saml.enabled">
                <span message-key="angular.saml2.directives.settings.ma-settings.activate"></span>
            </md-switch>

            <md-input-container class="md-block md-input-has-placeholder" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.identityProviderMetadata"></label>
                <input onchange="angular.element(this).scope().saml.fileNameChanged(this)" name="identityProviderMetadata"
                       type="file">
            </md-input-container>
            <md-input-container class="md-block" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.relyingPartyIdentifier"></label>
                <input ng-model="saml.relyingPartyIdentifier">
            </md-input-container>
            <div layout="row">
                <md-input-container class="md-block md-input-has-placeholder" flex>
                    <label message-key="angular.saml2.directives.settings.ma-settings.keyStore"></label>
                    <input onchange="angular.element(this).scope().saml.fileNameChanged(this)" name="keyStore" type="file">
                </md-input-container>

                <div flex="5"></div>

                <md-input-container class="md-block" flex>
                    <label message-key="angular.saml2.directives.settings.ma-settings.keyStoreType"></label>
                    <md-select ng-model="saml.keyStoreType">
                        <md-option ng-repeat="(key, value) in saml.availableKeyStoreTypes" ng-value="key">
                            {{ value }}
                        </md-option>
                    </md-select>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container class="md-block" flex>
                    <label message-key="angular.saml2.directives.settings.ma-settings.keyStoreAlias"></label>
                    <input ng-model="saml.keyStoreAlias">
                </md-input-container>
            </div>
            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="angular.saml2.directives.settings.ma-settings.keyStorePass"></label>
                    <input ng-model="saml.keyStorePass">
                </md-input-container>

                <div flex="5"></div>

                <md-input-container class="md-block" flex>
                    <label message-key="angular.saml2.directives.settings.ma-settings.privateKeyPass"></label>
                    <input ng-model="saml.privateKeyPass">
                </md-input-container>
            </div>
            <md-input-container class="md-block" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.incomingTargetUrl"></label>
                <input ng-model="saml.incomingTargetUrl">
            </md-input-container>
            <md-input-container class="md-block" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.postLoginPath"></label>
                <input ng-model="saml.postLoginPath">
            </md-input-container>
            <md-input-container class="md-block" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.maximumAuthenticationLifetime"></label>
                <input ng-model="saml.maximumAuthenticationLifetime">
            </md-input-container>
            <md-input-container class="md-block" flex>
                <md-checkbox ng-model="saml.forceAuth" ng-disabled="saml.passive" aria-label=""
                             ng-click="enable()">
                    <span message-key="angular.saml2.directives.settings.ma-settings.forceAuth"></span>
                </md-checkbox>
                <md-checkbox ng-model="saml.passive" ng-disabled="saml.forceAuth" aria-label=""
                             ng-click="enable()">
                    <span message-key="angular.saml2.directives.settings.ma-settings.passive"></span>
                </md-checkbox>
                <md-checkbox ng-model="saml.signAuthnRequest" aria-label="" ng-click="enable()">
                    <span message-key="angular.saml2.directives.settings.ma-settings.signAuthnRequest"></span>
                </md-checkbox>
                <md-checkbox ng-model="saml.requireSignedAssertions" aria-label="" ng-click="enable()">
                    <span message-key="angular.saml2.directives.settings.ma-settings.requireSignedAssertions"></span>
                </md-checkbox>
            </md-input-container>
            <md-input-container class="md-block" flex>
                <label message-key="angular.saml2.directives.settings.ma-settings.bindingType"></label>
                <md-select ng-model="saml.bindingType">
                    <md-option ng-repeat="(key, value) in saml.availableBindings" ng-value="key">
                        {{ value }}
                    </md-option>
                </md-select>
            </md-input-container>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" data-sel-role="metadata" message-key="angular.saml2.directives.settings.ma-settings.openmetadata"
                       ng-click="saml.metadata()"
                       ng-show="saml.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" data-sel-role="mappers" message-key="angular.saml2.directives.settings.ma-settings.mappers"
                       ng-click="saml.goToMappers()"
                       ng-show="saml.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" data-sel-role="save" message-key="angular.saml2.directives.settings.ma-settings.save"
                       ng-click="saml.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>