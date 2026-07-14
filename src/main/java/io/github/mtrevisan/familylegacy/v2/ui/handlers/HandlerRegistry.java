/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import java.util.HashMap;
import java.util.Map;


/**
 * Registry for RecordTypeHandler instances.
 * Maps record type names (e.g., "INDIVIDUAL") to their handlers.
 */
public final class HandlerRegistry{

	private static final Map<String, RecordTypeHandler<?>> handlers = new HashMap<>();


	private HandlerRegistry(){}

	/**
	 * Registers a handler for a record type.
	 *
	 * @param handler	The handler to register.
	 */
	public static void register(RecordTypeHandler<?> handler){
		handlers.putIfAbsent(handler.getType(), handler);
	}

	/**
	 * Returns the handler for the given record type.
	 *
	 * @param type	The record type (e.g., "INDIVIDUAL").
	 * @return	The handler, or null if not registered.
	 */
	public static RecordTypeHandler<?> getHandler(String type){
		return handlers.get(type);
	}

	/**
	 * Checks if a handler is registered for the given type.
	 */
	public static boolean isRegistered(String type){
		return handlers.containsKey(type);
	}

	/**
	 * Returns all registered handlers.
	 */
	public static Map<String, RecordTypeHandler<?>> getHandlers(){
		return new HashMap<>(handlers);
	}

}
