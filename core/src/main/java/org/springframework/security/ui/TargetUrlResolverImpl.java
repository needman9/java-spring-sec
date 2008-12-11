/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.Authentication;
import org.springframework.security.ui.savedrequest.SavedRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Default implementation for {@link TargetUrlResolver}
 * <p>
 * Returns a target URL based from the contents of the configured <tt>targetUrlParameter</tt> if present on
 * the current request. Failing that, the SavedRequest in the session will be used.
 *
 * @author Martino Piccinato
 * @author Luke Taylor
 * @version $Id$
 * @since 2.0
 *
 */
public class TargetUrlResolverImpl implements TargetUrlResolver {
    public static String DEFAULT_TARGET_PARAMETER = "spring-security-redirect";

    /* SEC-213 */
    private String targetUrlParameter = DEFAULT_TARGET_PARAMETER;

    /**
     * If <code>true</code>, will only use <code>SavedRequest</code> to determine the target URL on successful
     * authentication if the request that caused the authentication request was a GET.
     * It will then return null for a POST/PUT request.
     * Defaults to false.
     */
    private boolean justUseSavedRequestOnGet = false;

    public String determineTargetUrl(HttpServletRequest currentRequest, Authentication auth) {

        String targetUrl = currentRequest.getParameter(targetUrlParameter);

        if (StringUtils.hasText(targetUrl)) {
            try {
                return URLDecoder.decode(targetUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 not supported. Shouldn't be possible");
            }
        }

        SavedRequest savedRequest = getSavedRequest(currentRequest);

        if (savedRequest != null) {
            if (!justUseSavedRequestOnGet || savedRequest.getMethod().equals("GET")) {
                targetUrl = savedRequest.getFullRequestUrl();
            }
        }

        return targetUrl;
    }

    private static SavedRequest getSavedRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        SavedRequest savedRequest = (SavedRequest) session.getAttribute(SavedRequest.SPRING_SECURITY_SAVED_REQUEST_KEY);

        return savedRequest;
    }

    /**
     * @return <code>true</code> if just GET request will be used
     * to determine target URLs, <code>false</code> otherwise.
     */
    protected boolean isJustUseSavedRequestOnGet() {
        return justUseSavedRequestOnGet;
    }

    /**
     * @param justUseSavedRequestOnGet set to <code>true</code> if
     * just GET request will be used to determine target URLs,
     * <code>false</code> otherwise.
     */
    public void setJustUseSavedRequestOnGet(boolean justUseSavedRequestOnGet) {
        this.justUseSavedRequestOnGet = justUseSavedRequestOnGet;
    }


    /**
     * Before checking the SavedRequest, the current request will be checked for this parameter
     * and the value used as the target URL if resent.
     *
     *  @param targetUrlParameter the name of the parameter containing the encoded target URL. Defaults
     *  to "redirect".
     */
    public void setTargetUrlParameter(String targetUrlParameter) {
        Assert.hasText("targetUrlParamete canot be null or empty");
        this.targetUrlParameter = targetUrlParameter;
    }
}
