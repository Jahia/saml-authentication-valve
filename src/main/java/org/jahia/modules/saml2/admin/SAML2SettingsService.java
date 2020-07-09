package org.jahia.modules.saml2.admin;

import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.templates.JahiaModuleAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;

public final class SAML2SettingsService implements InitializingBean, JahiaModuleAware {

    private static final Logger logger = LoggerFactory.getLogger(SAML2SettingsService.class);
    private static final SAML2SettingsService INSTANCE = new SAML2SettingsService();
    private Map<String, SAML2Settings> settingsBySiteKeyMap = new HashMap<>();
    private String resourceBundleName;
    private JahiaTemplatesPackage module;
    private Set<String> supportedLocales = Collections.emptySet();
    private SAML2Util util;

    private SAML2SettingsService() {
        super();
    }

    public static SAML2SettingsService getInstance() {
        return INSTANCE;
    }

    public void loadSettings(final String siteKey) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {

            @Override
            public Object doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                //clean up
                if (siteKey == null) {
                    settingsBySiteKeyMap.clear();
                    for (final JCRSiteNode siteNode : JahiaSitesService.getInstance().getSitesNodeList(session)) {
                        loadSettings(siteNode);
                    }
                } else {
                    settingsBySiteKeyMap.remove(siteKey);
                    if (session.nodeExists("/sites/" + siteKey)) {
                        loadSettings(JahiaSitesService.getInstance().getSiteByKey(siteKey, session));
                    }
                }
                return null;
            }

            private void loadSettings(final JCRSiteNode siteNode) throws RepositoryException {
                boolean loaded;
                try {
                    final SAML2Settings settings = new SAML2Settings(siteNode.getSiteKey(), util);
                    loaded = settings.load();
                    if (loaded) {
                        settingsBySiteKeyMap.put(siteNode.getSiteKey(), settings);
                    }
                } catch (Exception e) {
                    logger.error("Error while loading settings from " + siteNode.getPath() + "/" + SAML2Constants.SETTINGS_NODE_NAME, e);
                }
            }
        });
    }

    public SAML2Settings setSAML2Settings(final String siteKey, final String identityProviderPath, final String relyingPartyIdentifier, final String incomingTargetUrl, final String spMetaDataLocation, final String keyStoreLocation, final String keyStorePass, final String privateKeyPass, final String postLoginPath, final Double maximumAuthentifcationLifetime) throws IOException {
        final SAML2Settings settings = new SAML2Settings(siteKey, util);
        settings.setIdentityProviderPath(identityProviderPath);
        settings.setRelyingPartyIdentifier(relyingPartyIdentifier);
        settings.setIncomingTargetUrl(incomingTargetUrl);
        settings.setSpMetaDataLocation(spMetaDataLocation);
        settings.setKeyStoreLocation(keyStoreLocation);
        settings.setKeyStorePass(keyStorePass);
        settings.setPrivateKeyPass(privateKeyPass);
        settings.setPostLoginPath(postLoginPath);
        settings.setMaximumAuthenticationLifetime(maximumAuthentifcationLifetime);

        // refresh and save settings
        settings.store();

        settingsBySiteKeyMap.put(siteKey, settings);
        return settings;
    }

    public void removeServerSettings(String siteKey) {
        if (settingsBySiteKeyMap.containsKey(siteKey)) {
            settingsBySiteKeyMap.get(siteKey).remove();
            settingsBySiteKeyMap.remove(siteKey);
        }
    }

    public Map<String, SAML2Settings> getSettingsBySiteKeyMap() {
        return settingsBySiteKeyMap;
    }

    public SAML2Settings getSettings(final String siteKey) {
        return settingsBySiteKeyMap.get(siteKey);
    }

    @Override
    public void setJahiaModule(final JahiaTemplatesPackage jahiaTemplatesPackage) {
        this.module = jahiaTemplatesPackage;

        final Resource[] resources;
        final String rbName = module.getResourceBundleName();
        if (rbName != null) {
            resourceBundleName = StringUtils.substringAfterLast(rbName, ".") + "-i18n";
            resources = module.getResources("javascript/i18n");
            supportedLocales = new HashSet<>();
            for (final Resource resource : resources) {
                final String fileName = resource.getFilename();
                if (fileName.startsWith(resourceBundleName)) {
                    final String l = StringUtils.substringBetween(fileName, resourceBundleName, ".js");
                    supportedLocales.add(l.length() > 0 ? StringUtils.substringAfter(l, "_") : StringUtils.EMPTY);
                }
            }
        }
    }

    public Set<String> getSupportedLocales() {
        return Collections.unmodifiableSet(supportedLocales);
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadSettings(null);
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
