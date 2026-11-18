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
package io.github.mtrevisan.calendars.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * A thread-safe cache of the values of a pure function that keeps at most a given number of entries, discarding the
 * least recently used one.
 * <p>
 * The values are computed outside the lock, so the function may use the same cache (or others); two threads asking for
 * the same missing key may both compute it, which is harmless for a pure function.
 * </p>
 *
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */
public final class BoundedCache<K, V>{

	private final Map<K, V> entries;


	/**
	 * @param maxEntries the maximum number of entries kept (greater than zero)
	 * @throws IllegalArgumentException if {@code maxEntries} is not positive
	 */
	public BoundedCache(final int maxEntries){
		if(maxEntries < 1)
			throw new IllegalArgumentException("The maximum number of entries must be positive: " + maxEntries);

		entries = new LinkedHashMap<>(16, 0.75f, true){
			@Override
			protected boolean removeEldestEntry(final Map.Entry<K, V> eldest){
				return (size() > maxEntries);
			}
		};
	}


	/**
	 * @param key    the key
	 * @param loader computes the value of a key (not {@code null} result)
	 * @return the cached value, or the one computed by the loader
	 */
	public V get(final K key, final Function<? super K, ? extends V> loader){
		synchronized(entries){
			final V cached = entries.get(key);
			if(cached != null)
				return cached;
		}

		final V value = loader.apply(key);
		synchronized(entries){
			entries.put(key, value);
		}
		return value;
	}

	/** @return the number of entries currently kept */
	public int size(){
		synchronized(entries){
			return entries.size();
		}
	}

}
