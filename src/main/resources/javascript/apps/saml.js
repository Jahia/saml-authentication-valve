window.jahia.i18n.loadNamespaces('saml-authentication-valve');

window.jahia.uiExtender.registry.add('adminRoute', 'settings/saml', {
    targets: ['administration-sites:80'],
    icon: window.jahia.moonstone.toIconComponent('Security'),
    label: 'saml-authentication-valve:label.title',
    isSelectable: true,
    requiredPermission: 'canSetupSaml2',
    requireModuleInstalledOnSite: 'saml-authentication-valve',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.saml2-settings.html'
});