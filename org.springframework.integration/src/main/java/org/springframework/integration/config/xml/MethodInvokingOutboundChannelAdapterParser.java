/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.consumer.MethodInvokingConsumer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected String parseAndRegisterConsumer(Element element, ParserContext parserContext) {
		String consumerRef = element.getAttribute("ref");
		Assert.isTrue(StringUtils.hasText(consumerRef), "The 'ref' attribute is required.");
		if (element.hasAttribute("method")) {
			consumerRef = BeanDefinitionReaderUtils.registerWithGeneratedName(
					this.parseConsumer(element, parserContext), parserContext.getRegistry());
		}
		return consumerRef;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingConsumer.class);
		invokerBuilder.addConstructorArgReference(element.getAttribute("ref"));
		invokerBuilder.addConstructorArgValue(element.getAttribute("method"));
		return invokerBuilder.getBeanDefinition();
	}

}
