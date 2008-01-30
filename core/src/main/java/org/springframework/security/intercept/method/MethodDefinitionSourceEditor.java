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

package org.springframework.security.intercept.method;

import org.springframework.security.ConfigAttributeDefinition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.LinkedHashMap;


/**
 * Property editor to assist with the setup of a {@link MethodDefinitionSource}.
 * <p>The class creates and populates a {@link MethodDefinitionMap}.</p>
 *
 * @author Ben Alex
 * @version $Id$
 */
public class MethodDefinitionSourceEditor extends PropertyEditorSupport {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(MethodDefinitionSourceEditor.class);

    //~ Methods ========================================================================================================

    public void setAsText(String s) throws IllegalArgumentException {
        if ((s == null) || "".equals(s)) {
            setValue(new MethodDefinitionMap());
            return;
        }

        // Use properties editor to tokenize the string
        PropertiesEditor propertiesEditor = new PropertiesEditor();
        propertiesEditor.setAsText(s);

        Properties props = (Properties) propertiesEditor.getValue();

        // Now we have properties, process each one individually
        Map mappings = new LinkedHashMap();

        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String value = props.getProperty(name);

            String[] tokens = StringUtils.commaDelimitedListToStringArray(value);

            mappings.put(name, new ConfigAttributeDefinition(tokens));
        }

        setValue(new MethodDefinitionMap(mappings));
    }
}
