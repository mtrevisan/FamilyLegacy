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
package io.github.mtrevisan.familylegacy.ui.features;

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * The single place that decides what the user interface shows, from the {@link UxLevel level} of the user (progressive disclosure).
 * <p>
 * Menus, toolbars and views ask this service instead of checking the level themselves. A feature declares the lowest level it is shown
 * at, either by registering an id ({@link #register(String, UxLevel)}) or, for a tool, with its own minimum level; a feature that is
 * not registered is shown at every level. The level is only about presentation: the model is complete at every level and nothing is
 * disabled, and the level lives in the user preferences, never in the records.
 * </p>
 */
public final class FeatureVisibilityService{

	/** The service of the application, that keeps the level in the user preferences. */
	private static final FeatureVisibilityService SHARED = new FeatureVisibilityService(
		() -> AppPreferences.getInstance().uxLevel(), level -> AppPreferences.getInstance().setUxLevel(level));


	private final Supplier<UxLevel> store;
	private final Consumer<UxLevel> saver;
	private final Map<String, UxLevel> features = new ConcurrentHashMap<>();
	private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
	private volatile UxLevel level;


	/**
	 * @param store	Where the level is read from (at the start).
	 * @param saver	Where the level is written to when it changes (a no-op for the tests, that never touch the preferences).
	 */
	public FeatureVisibilityService(final Supplier<UxLevel> store, final Consumer<UxLevel> saver){
		this.store = Objects.requireNonNull(store, "store");
		this.saver = Objects.requireNonNull(saver, "saver");
		level = Objects.requireNonNull(store.get(), "level");
	}

	/** @return a service that keeps the level in memory only, starting at {@code initial}. */
	public static FeatureVisibilityService inMemory(final UxLevel initial){
		return new FeatureVisibilityService(() -> initial, level -> {});
	}

	/** @return the service of the application. */
	public static FeatureVisibilityService shared(){
		return SHARED;
	}


	/** @return the current level. */
	public UxLevel level(){
		return level;
	}

	/**
	 * Changes the level, saves it, and tells the listeners (they rebuild what depends on it); nothing happens if it is the same.
	 *
	 * @param newLevel	The level to use from now on.
	 */
	public void setLevel(final UxLevel newLevel){
		Objects.requireNonNull(newLevel, "newLevel");
		if(newLevel == level)
			return;

		level = newLevel;
		saver.accept(newLevel);
		listeners.forEach(Runnable::run);
	}

	/**
	 * Registers a feature with the lowest level it is shown at, so that the features that come later need no change here.
	 *
	 * @param featureId	What identifies the feature (for example {@code "research.conclusions"}).
	 * @param minimum	The lowest level the feature is shown at.
	 */
	public void register(final String featureId, final UxLevel minimum){
		features.put(Objects.requireNonNull(featureId, "featureId"), Objects.requireNonNull(minimum, "minimum"));
	}

	/** @return the lowest level a feature is shown at: the one it registered, or the basic one if it did not. */
	public UxLevel minimumLevel(final String featureId){
		return features.getOrDefault(featureId, UxLevel.BASIC);
	}

	/** @return whether a feature with that id is shown at the current level. */
	public boolean isVisible(final String featureId){
		return isVisible(minimumLevel(featureId));
	}

	/** @return whether a feature that needs {@code minimum} is shown at the current level. */
	public boolean isVisible(final UxLevel minimum){
		return level.includes(minimum);
	}

	/** @param listener	What to do when the level changes (it is called on the thread that changed it). */
	public void addListener(final Runnable listener){
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public void removeListener(final Runnable listener){
		listeners.remove(listener);
	}

}
