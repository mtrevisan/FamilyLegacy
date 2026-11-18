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
package io.github.mtrevisan.familylegacy.ui.shell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** An automatic check is made at most once every 12 hours, and not at all if the user turned it off. */
class AutomaticUpdatePolicyTest{

	private static final long HOUR = 3_600_000L;

	private static final class FakeStore implements AutomaticUpdatePolicy.Store{

		boolean enabled = true;
		long last;

		@Override
		public boolean enabled(){
			return enabled;
		}

		@Override
		public long lastCheck(){
			return last;
		}

		@Override
		public void recordCheck(final long millis){
			last = millis;
		}

	}


	@Test
	void theFirstStartChecks(){
		Assertions.assertTrue(new AutomaticUpdatePolicy(new FakeStore(), () -> 1_000 * HOUR).isDue());
	}

	@Test
	void noSecondCheckWithinTheInterval(){
		final FakeStore store = new FakeStore();
		final long[] now = {1_000 * HOUR};
		final AutomaticUpdatePolicy policy = new AutomaticUpdatePolicy(store, () -> now[0]);

		policy.recordAttempt();
		Assertions.assertFalse(policy.isDue());
		now[0] += 11 * HOUR;
		Assertions.assertFalse(policy.isDue());
		now[0] += HOUR;
		Assertions.assertTrue(policy.isDue());
	}

	@Test
	void turnedOffNeverChecks(){
		final FakeStore store = new FakeStore();
		store.enabled = false;

		Assertions.assertFalse(new AutomaticUpdatePolicy(store, () -> 1_000 * HOUR).isDue());
	}

	@Test
	void aClockSetBackDoesNotBlockTheChecksForever(){
		final FakeStore store = new FakeStore();
		store.last = 2_000 * HOUR;

		Assertions.assertTrue(new AutomaticUpdatePolicy(store, () -> 1_000 * HOUR).isDue());
	}

}
