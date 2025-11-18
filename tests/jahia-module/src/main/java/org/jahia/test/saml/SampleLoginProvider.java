/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.test.saml;

import org.jahia.modules.saml2.SAML2InfoProvider;
import org.jahia.params.valves.LoginUrlProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Jerome Blanchard
 */
@Component(immediate = true)
public class SampleLoginProvider implements LoginUrlProvider {

    @Reference SAML2InfoProvider saml2InfoProvider;

    @Override public String getLoginUrl(HttpServletRequest request) {
        String callbackUrl = saml2InfoProvider.getCallbackUrl(request);
        return request.getContextPath().concat("/modules/saml-authentication-valve-test-module/html/login.html?callback=").concat(callbackUrl);
    }

    @Override public boolean hasCustomLoginUrl() {
        return true;
    }

}
