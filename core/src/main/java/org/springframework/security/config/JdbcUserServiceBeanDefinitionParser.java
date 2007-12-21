package org.springframework.security.config;

import org.springframework.security.userdetails.jdbc.JdbcUserDetailsManager;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import org.w3c.dom.Element;

/**
 * @author Luke Taylor
 * @version $Id$
 */
public class JdbcUserServiceBeanDefinitionParser extends AbstractUserDetailsServiceBeanDefinitionParser {
	static final String ATT_DATA_SOURCE = "data-source";

    protected Class getBeanClass(Element element) {
        return JdbcUserDetailsManager.class;
    }

    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        // TODO: Set authenticationManager property
        String dataSource = element.getAttribute(ATT_DATA_SOURCE);
        // An explicit dataSource was specified, so use it
        if (dataSource != null) {
            builder.addPropertyReference("dataSource", dataSource);
        } else {
            // TODO: Have some sensible fallback if dataSource not specified, eg autowire
            throw new SecurityConfigurationException(ATT_DATA_SOURCE  + " is required for "
                    + Elements.JDBC_USER_SERVICE );
        }
    }
}
