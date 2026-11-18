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

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;

import java.time.Duration;
import java.util.function.LongSupplier;


/**
 * When the application looks for a newer release by itself, at startup.
 * <p>
 * GitHub answers an anonymous client at most 60 requests an hour for each address (shared by everybody behind the same router), and
 * a check costs one. So the application asks at most once every {@link #INTERVAL}, whatever the number of starts, and writes the time
 * down <em>before</em> asking: a failed or interrupted attempt is not repeated at the next start. The user can turn it off.
 * </p>
 */
final class AutomaticUpdatePolicy{

	/** The least time between two automatic checks. */
	static final Duration INTERVAL = Duration.ofHours(12);


	/** Where the choice of the user and the time of the last check are kept. */
	interface Store{

		boolean enabled();

		/** @return	When the last check was made (epoch milliseconds), 0 if never. */
		long lastCheck();

		void recordCheck(long millis);

	}


	private final Store store;
	private final LongSupplier clock;


	/** @return	The policy of the application, kept in its preferences. */
	static AutomaticUpdatePolicy application(){
		final AppPreferences preferences = AppPreferences.getInstance();
		return new AutomaticUpdatePolicy(new Store(){
			@Override
			public boolean enabled(){
				return preferences.updateCheckEnabled();
			}

			@Override
			public long lastCheck(){
				return preferences.lastUpdateCheck();
			}

			@Override
			public void recordCheck(final long millis){
				preferences.setLastUpdateCheck(millis);
			}
		}, System::currentTimeMillis);
	}

	AutomaticUpdatePolicy(final Store store, final LongSupplier clock){
		this.store = store;
		this.clock = clock;
	}

	/** @return	Whether a check should be made now: it is enabled and the last one is old enough (or the clock was set back). */
	boolean isDue(){
		if(!store.enabled())
			return false;

		final long now = clock.getAsLong();
		final long last = store.lastCheck();
		return (last <= 0 || last > now || now - last >= INTERVAL.toMillis());
	}

	/** Writes down that a check is made now. */
	void recordAttempt(){
		store.recordCheck(clock.getAsLong());
	}

}
