/* Copyright 2004 Acegi Technology Pty Limited
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

package net.sf.acegisecurity.providers.dao;

import org.apache.commons.codec.digest.DigestUtils;


/**
 * <p>
 * SHA implementation of PasswordEncoder.<br/
 * > The ignorePasswordCase parameter is not used for this implementation.<br/
 * > A null password is encoded to the same value as an empty ("") password.
 * </p>
 *
 * @author colin sampaleanu
 * @version $Id$
 */
public class SHAPasswordEncoder implements PasswordEncoder {
    //~ Methods ================================================================

    /* (non-Javadoc)
     * @see net.sf.acegisecurity.providers.dao.PasswordEncoder#isPasswordValid(net.sf.acegisecurity.providers.dao.User, java.lang.String, boolean)
     */
    public boolean isPasswordValid(String encPass, String rawPass,
        Object saltSource, boolean ignorePasswordCase) {
        String pass1 = "" + encPass;
        String pass2 = DigestUtils.shaHex("" + rawPass);

        return pass1.equals(pass2);
    }

    /* (non-Javadoc)
     * @see net.sf.acegisecurity.providers.dao.PasswordEncoder#encodePassword(java.lang.String, java.lang.Object)
     */
    public String encodePassword(String rawPass, Object saltSource) {
        return DigestUtils.shaHex("" + rawPass);
    }
}
