<%@ page language="java" contentType="text/html;charset=UTF-8" %>
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

<template:addResources type="javascript" resources="
    saml2/services/ma-settings-services.js,
    saml2/services/helper-service.js,
    saml2/directives/settings/ma-settings.js"/>

<template:addResources>
    <script>
        angular.module('JahiaOAuthApp').constant('jahiaContext', {
            siteKey: '${renderContext.site.siteKey}',
            baseEdit: '${url.context}${url.baseEdit}',
            context: '${url.context}',
            sitePath: '${renderContext.siteInfo.sitePath}'
        });
    </script>
</template:addResources>


<ma-settings ng-cloak></ma-settings>

<div layout="row" layout-align="center" flex="100">
    <%--<md-subheader class="ma-background"><fmt:message key="jahia.copyright" />${' - '}<fmt:message key="jahia.company" /></md-subheader>--%>
</div>

<script type="text/ng-template" id="mappers.html">
    <jcr:sql var="oauthMappersViews"
             sql="SELECT * FROM [jmix:authMapperSettingView] as mapperView WHERE ISDESCENDANTNODE(mapperView, '/modules')"/>
    <c:set var="siteHasMapper" value="false"/>
    <c:forEach items="${oauthMappersViews.nodes}" var="mapperView">
        <c:set var="siteHasMapper" value="true"/>
        <template:module node="${mapperView}"/>
    </c:forEach>
    <c:if test="${not siteHasMapper}">
        <md-card>
            <md-card-content>
                <span message-key="joant_oauthConnectorSiteSettings.mapper.notFound"></span>
            </md-card-content>
        </md-card>
    </c:if>
</script>