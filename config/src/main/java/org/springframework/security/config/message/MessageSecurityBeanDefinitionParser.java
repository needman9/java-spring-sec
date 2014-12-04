/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.security.access.vote.ConsensusBased;
import org.springframework.security.config.Elements;
import org.springframework.security.messaging.access.expression.ExpressionBasedMessageSecurityMetadataSourceFactory;
import org.springframework.security.messaging.access.expression.MessageExpressionVoter;
import org.springframework.security.messaging.access.intercept.ChannelSecurityInterceptor;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.messaging.util.matcher.SimpDestinationMessageMatcher;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Parses Spring Security's message namespace support. A simple example is:
 *
 * <code>
 * &lt;messages&gt;
 *     &lt;message-interceptor pattern='/permitAll' access='permitAll' /&gt;
 *     &lt;message-interceptor pattern='/denyAll' access='denyAll' /&gt;
 * &lt;/messages&gt;
 * </code>
 *
 * <p>
 * The above configuration will ensure that any SimpAnnotationMethodMessageHandler has the AuthenticationPrincipalArgumentResolver
 * registered as a custom argument resolver. It also ensures that the SecurityContextChannelInterceptor is automatically
 * registered for the clientInboundChannel. Last, it ensures that a ChannelSecurityInterceptor is registered with the
 * clientInboundChannel.
 * </p>
 *
 * <p>
 * If finer control is necessary, the id attribute can be used as shown below:
 * </p>
 *
 * <code>
 * &lt;messages id="channelSecurityInterceptor"&gt;
 *     &lt;message-interceptor pattern='/permitAll' access='permitAll' /&gt;
 *     &lt;message-interceptor pattern='/denyAll' access='denyAll' /&gt;
 * &lt;/messages&gt;
 * </code>
 *
 * <p>
 * Now the configuration will only create a bean named ChannelSecurityInterceptor and assign it to the id of
 * channelSecurityInterceptor. Users can explicitly wire Spring Security using the standard Spring Messaging XML
 * namespace support.
 * </p>
 *
 * @author Rob Winch
 * @since 4.0
 */
public final class MessageSecurityBeanDefinitionParser implements BeanDefinitionParser {
    private static final Log logger = LogFactory.getLog(MessageSecurityBeanDefinitionParser.class);

    private static final String ID_ATTR = "id";

    private static final String PATTERN_ATTR = "pattern";

    private static final String ACCESS_ATTR = "access";


    /**
     * @param element
     * @param parserContext
     * @return
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        BeanDefinitionRegistry registry = parserContext.getRegistry();
        XmlReaderContext context = parserContext.getReaderContext();

        ManagedMap<BeanDefinition,String> matcherToExpression = new ManagedMap<BeanDefinition, String>();

        String id = element.getAttribute(ID_ATTR);

        List<Element> interceptMessages = DomUtils.getChildElementsByTagName(element, Elements.INTERCEPT_MESSAGE);
        for(Element interceptMessage : interceptMessages) {
            String matcherPattern = interceptMessage.getAttribute(PATTERN_ATTR);
            String accessExpression = interceptMessage.getAttribute(ACCESS_ATTR);
            BeanDefinitionBuilder matcher = BeanDefinitionBuilder.rootBeanDefinition(SimpDestinationMessageMatcher.class);
            matcher.addConstructorArgValue(matcherPattern);
            matcherToExpression.put(matcher.getBeanDefinition(), accessExpression);
        }

        BeanDefinitionBuilder mds = BeanDefinitionBuilder.rootBeanDefinition(ExpressionBasedMessageSecurityMetadataSourceFactory.class);
        mds.setFactoryMethod("createExpressionMessageMetadataSource");
        mds.addConstructorArgValue(matcherToExpression);

        String mdsId = context.registerWithGeneratedName(mds.getBeanDefinition());

        ManagedList<BeanDefinition> voters = new ManagedList<BeanDefinition>();
        voters.add(new RootBeanDefinition(MessageExpressionVoter.class));
        BeanDefinitionBuilder adm = BeanDefinitionBuilder.rootBeanDefinition(ConsensusBased.class);
        adm.addConstructorArgValue(voters);

        BeanDefinitionBuilder inboundChannelSecurityInterceptor = BeanDefinitionBuilder.rootBeanDefinition(ChannelSecurityInterceptor.class);
        inboundChannelSecurityInterceptor.addConstructorArgValue(registry.getBeanDefinition(mdsId));
        inboundChannelSecurityInterceptor.addPropertyValue("accessDecisionManager", adm.getBeanDefinition());
        String inSecurityInterceptorName = context.registerWithGeneratedName(inboundChannelSecurityInterceptor.getBeanDefinition());

        if(StringUtils.hasText(id)) {
            registry.registerAlias(inSecurityInterceptorName, id);
        } else {
            BeanDefinitionBuilder mspp = BeanDefinitionBuilder.rootBeanDefinition(MessageSecurityPostProcessor.class);
            mspp.addConstructorArgValue(inSecurityInterceptorName);
            context.registerWithGeneratedName(mspp.getBeanDefinition());
        }

        return null;
    }

    static class MessageSecurityPostProcessor implements BeanDefinitionRegistryPostProcessor {
        private static final String CLIENT_INBOUND_CHANNEL_BEAN_ID = "clientInboundChannel";

        private static final String INTERCEPTORS_PROP = "interceptors";

        private static final String CUSTOM_ARG_RESOLVERS_PROP = "customArgumentResolvers";

        private final String inboundSecurityInterceptorId;

        public MessageSecurityPostProcessor(String inboundSecurityInterceptorId) {
            this.inboundSecurityInterceptorId = inboundSecurityInterceptorId;
        }

        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            String[] beanNames = registry.getBeanDefinitionNames();
            for(String beanName : beanNames) {
                BeanDefinition bd = registry.getBeanDefinition(beanName);
                if(bd.getBeanClassName().equals(SimpAnnotationMethodMessageHandler.class.getName())) {
                    PropertyValue current = bd.getPropertyValues().getPropertyValue(CUSTOM_ARG_RESOLVERS_PROP);
                    ManagedList<Object> argResolvers = new ManagedList<Object>();
                    if(current != null) {
                        argResolvers.addAll((ManagedList<?>)current.getValue());
                    }
                    argResolvers.add(new RootBeanDefinition(AuthenticationPrincipalArgumentResolver.class));
                    bd.getPropertyValues().add(CUSTOM_ARG_RESOLVERS_PROP, argResolvers);
                }
            }

            if(!registry.containsBeanDefinition(CLIENT_INBOUND_CHANNEL_BEAN_ID)) {
                return;
            }
            ManagedList<Object> interceptors = new ManagedList();
            interceptors.add(new RootBeanDefinition(SecurityContextChannelInterceptor.class));
            interceptors.add(registry.getBeanDefinition(inboundSecurityInterceptorId));

            BeanDefinition inboundChannel = registry.getBeanDefinition(CLIENT_INBOUND_CHANNEL_BEAN_ID);
            PropertyValue currentInterceptorsPv =  inboundChannel.getPropertyValues().getPropertyValue(INTERCEPTORS_PROP);
            if(currentInterceptorsPv != null) {
                ManagedList<?> currentInterceptors = (ManagedList<?>) currentInterceptorsPv.getValue();
                interceptors.addAll(currentInterceptors);
            }

            inboundChannel.getPropertyValues().add(INTERCEPTORS_PROP, interceptors);
        }

        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        }
    }
}