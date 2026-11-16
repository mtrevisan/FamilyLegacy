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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.cache.BoundedCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;


class BoundedCacheTest{

	@Test
	void computesOnlyOnceWhileKept(){
		final BoundedCache<Integer, Integer> cache = new BoundedCache<>(4);
		final AtomicInteger calls = new AtomicInteger();

		Assertions.assertEquals(10, cache.get(5, key -> {
			calls.incrementAndGet();
			return key * 2;
		}));
		Assertions.assertEquals(10, cache.get(5, key -> {
			calls.incrementAndGet();
			return key * 2;
		}));

		Assertions.assertEquals(1, calls.get());
	}

	@Test
	void neverExceedsTheLimit(){
		final BoundedCache<Integer, Integer> cache = new BoundedCache<>(3);
		for(int i = 0; i < 100; i ++)
			cache.get(i, key -> key);

		Assertions.assertEquals(3, cache.size());
	}

	@Test
	void discardsTheLeastRecentlyUsed(){
		final BoundedCache<Integer, Integer> cache = new BoundedCache<>(2);
		final AtomicInteger calls = new AtomicInteger();
		cache.get(1, key -> key);
		cache.get(2, key -> key);
		// 1 becomes the most recently used, so 2 is the one discarded
		cache.get(1, key -> key);
		cache.get(3, key -> key);

		cache.get(1, key -> {
			calls.incrementAndGet();
			return key;
		});
		Assertions.assertEquals(0, calls.get());
		cache.get(2, key -> {
			calls.incrementAndGet();
			return key;
		});
		Assertions.assertEquals(1, calls.get());
	}

	@Test
	void theLoaderMayUseTheSameCache(){
		final BoundedCache<Integer, Integer> cache = new BoundedCache<>(8);

		Assertions.assertEquals(8, cache.get(5, key -> cache.get(3, other -> other + key)));
	}

	@Test
	void rejectsANonPositiveLimit(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> new BoundedCache<Integer, Integer>(0));
	}

}
