/* Copyright 2004, 2005 Acegi Technology Pty Limited
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

package net.sf.acegisecurity.ui.x509;

import net.sf.acegisecurity.intercept.web.AuthenticationEntryPoint;
import net.sf.acegisecurity.AuthenticationException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * In the X.509 authentication case (unlike CAS, for example) the certificate will already
 * have been extracted from the request and a secure context established by the time
 * the security-enforcement filter is invoked.
 * <p>
 * Therefore this class isn't actually responsible for the commencement of authentication, as it
 * is in the case of other providers. It will be called if the certificate was rejected by
 * Acegi's X509AuthenticationProvider, resulting in a null authentication.
 * </p>
 * The <code>commence</code> method will always return an
 * <code>HttpServletResponse.SC_FORBIDDEN</code> (403 error).
 *
 *
 * @author Luke Taylor
 * @version $Id$
 * @see net.sf.acegisecurity.intercept.web.SecurityEnforcementFilter
 */
public class X509ProcessingFilterEntryPoint implements AuthenticationEntryPoint {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(X509ProcessingFilterEntryPoint.class);

    //~ Methods ================================================================
    
    /**
     * Returns a 403 error code to the client.
     */
    public void commence(ServletRequest request, ServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        logger.debug("X509 entry point called. Rejecting access");
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
    }
}
