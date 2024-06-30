/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.utilities.eventbus;

import io.github.mtrevisan.familylegacy.ui.utilities.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;


/**
 * An {@link EventBusInterface} factory that will return a singleton implementation loaded via the Java 6
 * {@link ServiceLoader}. By default, without changes to the jar, a {@link BasicEventBus} implementation
 * will be returned via the factory methods.
 * <p>
 * This static factory also includes the same methods as EventBus which will delegate to the ServiceLoader
 * loaded instance. Thus, the class creates a convenient single location for which client code can be hooked
 * to the configured EventBus.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public final class EventBusService{

	private static final Logger LOGGER = LoggerFactory.getLogger(EventBusService.class);


	private static final EventBusInterface EVENT_BUS = new BasicEventBus(false);
	static{
		EVENT_BUS.start();

		LOGGER.debug("Event bus started");
	}


	private EventBusService(){}


	public static void subscribe(final Object subscriber){
		EVENT_BUS.subscribe(subscriber);

		LOGGER.debug("Subscribed: '{}', called from {}", subscriber.getClass().getSimpleName(),
			LoggerHelper.extractCallerClasses()[1].getSimpleName());
	}

	public static void unsubscribe(final Object subscriber){
		EVENT_BUS.unsubscribe(subscriber);

		LOGGER.debug("Unsubscribed: '{}', called from {}", subscriber.getClass().getSimpleName(),
			LoggerHelper.extractCallerClasses()[1].getSimpleName());
	}

	public static void publish(final Object event){
		LOGGER.debug("Event published: '{}', called from {}", event, LoggerHelper.extractCallerClasses()[1].getSimpleName());

		EVENT_BUS.publish(event);
	}

	public static boolean hasPendingEvents(final Object event){
		return EVENT_BUS.hasPendingEvents(event);
	}

	public static boolean hasPendingEvents(){
		return EVENT_BUS.hasPendingEvents();
	}

}
