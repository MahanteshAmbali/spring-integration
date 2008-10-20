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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.consumer.ServiceActivatingConsumer;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorMethodResolutionTests {

	@Test
	public void singleAnnotationMatches() {
		SingleAnnotationTestBean testBean = new SingleAnnotationTestBean();
		ServiceActivatingConsumer serviceActivator = new ServiceActivatingConsumer(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.onMessage(new StringMessage("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotationFails() {
		MultipleAnnotationTestBean testBean = new MultipleAnnotationTestBean();
		new ServiceActivatingConsumer(testBean);
	}

	@Test
	public void singlePublicMethodMatches() {
		SinglePublicMethodTestBean testBean = new SinglePublicMethodTestBean();
		ServiceActivatingConsumer serviceActivator = new ServiceActivatingConsumer(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.onMessage(new StringMessage("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethodFails() {
		MultiplePublicMethodTestBean testBean = new MultiplePublicMethodTestBean();
		new ServiceActivatingConsumer(testBean);
	}


	private static class SingleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class MultipleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@ServiceActivator
		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class SinglePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class MultiplePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

}
