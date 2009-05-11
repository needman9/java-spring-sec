package org.springframework.security.config;

import java.util.List;

import org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Luke Taylor
 * @author Ben Alex
 *
 * @version $Id$
 */
public class InterceptMethodsBeanDefinitionDecorator implements BeanDefinitionDecorator {
    private BeanDefinitionDecorator delegate = new InternalInterceptMethodsBeanDefinitionDecorator();

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        ConfigUtils.registerProviderManagerIfNecessary(parserContext);
        ConfigUtils.registerDefaultMethodAccessManagerIfNecessary(parserContext);

        return delegate.decorate(node, definition, parserContext);
    }
}

/**
 * This is the real class which does the work. We need access to the ParserContext in order to do bean
 * registration.
 */
class InternalInterceptMethodsBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {
    static final String ATT_METHOD = "method";
    static final String ATT_ACCESS = "access";
    private static final String ATT_ACCESS_MGR = "access-decision-manager-ref";

    protected BeanDefinition createInterceptorDefinition(Node node) {
        Element interceptMethodsElt = (Element)node;
        BeanDefinitionBuilder interceptor = BeanDefinitionBuilder.rootBeanDefinition(MethodSecurityInterceptor.class);

        // Default to autowiring to pick up after invocation mgr
        interceptor.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);

        String accessManagerId = interceptMethodsElt.getAttribute(ATT_ACCESS_MGR);

        if (!StringUtils.hasText(accessManagerId)) {
            accessManagerId = BeanIds.METHOD_ACCESS_MANAGER;
        }

        interceptor.addPropertyValue("accessDecisionManager", new RuntimeBeanReference(accessManagerId));
        interceptor.addPropertyValue("authenticationManager", new RuntimeBeanReference(BeanIds.AUTHENTICATION_MANAGER));

        // Lookup parent bean information
        Element parent = (Element) node.getParentNode();
        String parentBeanClass = parent.getAttribute("class");
        parent = null;

        // Parse the included methods
        List<Element> methods = DomUtils.getChildElementsByTagName(interceptMethodsElt, Elements.PROTECT);

        StringBuffer sb = new StringBuffer();

        for (Element protectmethodElt : methods) {
            String accessConfig = protectmethodElt.getAttribute(ATT_ACCESS);

            // Support inference of class names
            String methodName = protectmethodElt.getAttribute(ATT_METHOD);

            if (methodName.lastIndexOf(".") == -1) {
                if (parentBeanClass != null && !"".equals(parentBeanClass)) {
                    methodName = parentBeanClass + "." + methodName;
                }
            }

            // Rely on the default property editor for MethodSecurityInterceptor.setSecurityMetadataSource to setup the MethodSecurityMetadataSource
            sb.append(methodName + "=" + accessConfig).append("\r\n");
        }

        interceptor.addPropertyValue("securityMetadataSource", sb.toString());

        return interceptor.getBeanDefinition();
    }
}
