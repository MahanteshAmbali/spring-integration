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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.consumer.AbstractReplyProducingMessageConsumer;
import org.springframework.integration.consumer.ServiceActivatingConsumer;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.TestHandlers;
import org.springframework.integration.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class SimpleDispatcherTests {

	@Test
	public void singleMessage() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		dispatcher.addConsumer(createConsumer(TestHandlers.countDownHandler(latch)));
		dispatcher.dispatch(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
	}

	@Test
	public void pointToPoint() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		dispatcher.addConsumer(createConsumer(TestHandlers.countingCountDownHandler(counter1, latch)));
		dispatcher.addConsumer(createConsumer(TestHandlers.countingCountDownHandler(counter2, latch)));
		dispatcher.dispatch(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("only 1 handler should have received the message", 1, counter1.get() + counter2.get());
	}

	@Test
	public void noDuplicateSubscriptions() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target);
		dispatcher.addConsumer(target);
		try {
			dispatcher.dispatch(new StringMessage("test"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals("target should not have duplicate subscriptions", 1, counter.get());
	}

	@Test
	public void removeConsumerBeforeSend() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target1 = new CountingTestEndpoint(counter, false);
		MessageConsumer target2 = new CountingTestEndpoint(counter, false);
		MessageConsumer target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target1);
		dispatcher.addConsumer(target2);
		dispatcher.addConsumer(target3);
		dispatcher.removeConsumer(target2);
		try {
			dispatcher.dispatch(new StringMessage("test"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(2, counter.get());
	}

	@Test
	public void removeConsumerBetweenSends() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target1 = new CountingTestEndpoint(counter, false);
		MessageConsumer target2 = new CountingTestEndpoint(counter, false);
		MessageConsumer target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target1);
		dispatcher.addConsumer(target2);
		dispatcher.addConsumer(target3);
		try {
			dispatcher.dispatch(new StringMessage("test1"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(3, counter.get());
		dispatcher.removeConsumer(target2);
		try {
			dispatcher.dispatch(new StringMessage("test2"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(5, counter.get());
		dispatcher.removeConsumer(target1);
		try {
			dispatcher.dispatch(new StringMessage("test3"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(6, counter.get());
	}

	@Test(expected = MessageDeliveryException.class)
	public void removeConsumerLastTargetCausesDeliveryException() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target);
		try {
			dispatcher.dispatch(new StringMessage("test1"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(1, counter.get());
		dispatcher.removeConsumer(target);
		dispatcher.dispatch(new StringMessage("test2"));
	}

	@Test
	public void handlersWithSelectorsAndOneAccepts() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger selectorCounter = new AtomicInteger();
		AbstractReplyProducingMessageConsumer consumer1 = createConsumer(TestHandlers.countingCountDownHandler(counter1, latch));
		AbstractReplyProducingMessageConsumer consumer2 = createConsumer(TestHandlers.countingCountDownHandler(counter2, latch));
		AbstractReplyProducingMessageConsumer consumer3 = createConsumer(TestHandlers.countingCountDownHandler(counter3, latch));
		consumer1.setSelector(new TestMessageSelector(selectorCounter, false));
		consumer2.setSelector(new TestMessageSelector(selectorCounter, false));
		consumer3.setSelector(new TestMessageSelector(selectorCounter, true));
		dispatcher.addConsumer(consumer1);
		dispatcher.addConsumer(consumer2);
		dispatcher.addConsumer(consumer3);
		dispatcher.dispatch(new StringMessage("test"));
		assertEquals(0, latch.getCount());
		assertEquals("selectors should have been invoked one time each", 3, selectorCounter.get());
		assertEquals("consumer with rejecting selector should not have received the message", 0, counter1.get());
		assertEquals("consumer with rejecting selector should not have received the message", 0, counter2.get());
		assertEquals("consumer with accepting selector should have received the message", 1, counter3.get());	
	}

	@Test
	public void handlersWithSelectorsAndNoneAccept() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger selectorCounter = new AtomicInteger();
		AbstractReplyProducingMessageConsumer consumer1 = createConsumer(TestHandlers.countingCountDownHandler(counter1, latch));
		AbstractReplyProducingMessageConsumer consumer2 = createConsumer(TestHandlers.countingCountDownHandler(counter2, latch));
		AbstractReplyProducingMessageConsumer consumer3 = createConsumer(TestHandlers.countingCountDownHandler(counter3, latch));
		consumer1.setSelector(new TestMessageSelector(selectorCounter, false));
		consumer2.setSelector(new TestMessageSelector(selectorCounter, false));
		consumer3.setSelector(new TestMessageSelector(selectorCounter, false));
		dispatcher.addConsumer(consumer1);
		dispatcher.addConsumer(consumer2);
		dispatcher.addConsumer(consumer3);
		boolean exceptionThrown = false;
		try {
			dispatcher.dispatch(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		assertEquals("selectors should have been invoked one time each", 3, selectorCounter.get());
		assertEquals("consumer with rejecting selector should not have received the message", 0, counter1.get());
		assertEquals("consumer with rejecting selector should not have received the message", 0, counter2.get());
		assertEquals("consumer with rejecting selector should not have received the message", 0, counter3.get());
	}

	@Test
	public void firstHandlerReturnsTrue() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target1 = new CountingTestEndpoint(counter, true);
		MessageConsumer target2 = new CountingTestEndpoint(counter, false);
		MessageConsumer target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target1);
		dispatcher.addConsumer(target2);
		dispatcher.addConsumer(target3);
		assertTrue(dispatcher.dispatch(new StringMessage("test")));
		assertEquals("only the first target should have been invoked", 1, counter.get());
	}

	@Test
	public void middleHandlerReturnsTrue() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target1 = new CountingTestEndpoint(counter, false);
		MessageConsumer target2 = new CountingTestEndpoint(counter, true);
		MessageConsumer target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target1);
		dispatcher.addConsumer(target2);
		dispatcher.addConsumer(target3);
		assertTrue(dispatcher.dispatch(new StringMessage("test")));
		assertEquals("first two targets should have been invoked", 2, counter.get());
	}

	@Test
	public void allHandlersReturnFalse() {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageConsumer target1 = new CountingTestEndpoint(counter, false);
		MessageConsumer target2 = new CountingTestEndpoint(counter, false);
		MessageConsumer target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addConsumer(target1);
		dispatcher.addConsumer(target2);
		dispatcher.addConsumer(target3);
		try {
			assertFalse(dispatcher.dispatch(new StringMessage("test")));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals("each target should have been invoked", 3, counter.get());
	}


	private static ServiceActivatingConsumer createConsumer(Object object) {
		return new ServiceActivatingConsumer(object);
	}


	private static class TestMessageSelector implements MessageSelector {

		private final AtomicInteger counter;

		private final boolean accept;

		TestMessageSelector(AtomicInteger counter, boolean accept) {
			this.counter = counter;
			this.accept = accept;
		}

		public boolean accept(Message<?> message) {
			this.counter.incrementAndGet();
			return this.accept;
		}
	}


	private static class CountingTestEndpoint implements MessageConsumer {

		private final AtomicInteger counter;

		private final boolean shouldAccept;

		CountingTestEndpoint(AtomicInteger counter, boolean shouldAccept) {
			this.counter = counter;
			this.shouldAccept = shouldAccept;
		}

		public void onMessage(Message<?> message) {
			this.counter.incrementAndGet();
			if (!this.shouldAccept) {
				throw new MessageRejectedException(message, "intentional test failure");
			}
		}
	}

}
