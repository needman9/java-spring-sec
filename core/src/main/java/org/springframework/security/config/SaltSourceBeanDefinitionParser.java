package org.springframework.security.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.providers.dao.salt.ReflectionSaltSource;
import org.springframework.security.providers.dao.salt.SystemWideSaltSource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Luke Taylor
 * @version $Id$
 * @since 2.0
 */
public class SaltSourceBeanDefinitionParser implements BeanDefinitionParser {
    static final String ATT_USER_PROPERTY = "user-property";
    static final String ATT_SYSTEM_WIDE = "system-wide";

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition saltSource;
        String userProperty = element.getAttribute(ATT_USER_PROPERTY);

        if (StringUtils.hasText(userProperty)) {
            saltSource = new RootBeanDefinition(ReflectionSaltSource.class);
            saltSource.getPropertyValues().addPropertyValue("userPropertyToUse", userProperty);
            saltSource.setSource(parserContext.extractSource(element));
            saltSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            
            return saltSource;
        }

        String systemWideSalt = element.getAttribute(ATT_SYSTEM_WIDE);

        if (StringUtils.hasText(systemWideSalt)) {
            saltSource = new RootBeanDefinition(SystemWideSaltSource.class);
            saltSource.getPropertyValues().addPropertyValue("systemWideSalt", systemWideSalt);
            saltSource.setSource(parserContext.extractSource(element));
            saltSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

            return saltSource;
        }

        parserContext.getReaderContext().error(Elements.SALT_SOURCE + " requires an attribute", element);
        return null;
    }
}
