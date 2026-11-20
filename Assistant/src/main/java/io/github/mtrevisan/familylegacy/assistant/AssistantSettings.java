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
package io.github.mtrevisan.familylegacy.assistant;

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * What the user decided about the Research Assistant itself (not about the research): whether the indicator of the toolbar is shown and
 * whether it blinks on a new error, whether the palette opens again after an action, and which assistants look for findings (for the
 * palette, and for the indicator).
 * <p>
 * They are a presentation choice, kept in the user preferences and never in the genealogy. Switching an assistant off only saves its
 * time: its tools stay in the palette, and nothing is removed from the file.
 * </p>
 */
public final class AssistantSettings{

	/** Where the settings are kept. */
	public interface Storage{

		/** @return the text kept for the key, or {@code defaultValue} */
		String get(String key, String defaultValue);

		/** Keeps the text of a key (an empty text forgets it). */
		void put(String key, String value);

	}


	static final String KEY_INDICATOR_VISIBLE = "assistant.indicator.visible";
	static final String KEY_INDICATOR_BLINK = "assistant.indicator.blink";
	static final String KEY_REOPEN = "assistant.reopen";
	static final String KEY_STATISTICS = "assistant.statistics";
	static final String KEY_DISABLED = "assistant.disabled";
	static final String KEY_INDICATOR_DISABLED = "assistant.indicator.disabled";
	private static final char SEPARATOR = ',';

	private final Storage storage;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();


	public AssistantSettings(final Storage storage){
		this.storage = storage;
	}

	/** @return the settings kept in the user preferences */
	public static AssistantSettings usingPreferences(){
		return new AssistantSettings(new Storage(){
			@Override
			public String get(final String key, final String defaultValue){
				return AppPreferences.getInstance().stringValue(key, defaultValue);
			}

			@Override
			public void put(final String key, final String value){
				AppPreferences.getInstance().setStringValue(key, value);
			}
		});
	}

	/** @return settings that keep nothing outside of memory: the tests must not write the preferences of the user */
	public static AssistantSettings inMemory(){
		final Map<String, String> kept = new HashMap<>();
		return new AssistantSettings(new Storage(){
			@Override
			public String get(final String key, final String defaultValue){
				return kept.getOrDefault(key, defaultValue);
			}

			@Override
			public void put(final String key, final String value){
				if(value == null || value.isEmpty())
					kept.remove(key);
				else
					kept.put(key, value);
			}
		});
	}


	/** @return whether the indicator is shown in the toolbar (and so, whether the findings are computed in the background) */
	public synchronized boolean isIndicatorVisible(){
		return flag(KEY_INDICATOR_VISIBLE, true);
	}

	public void setIndicatorVisible(final boolean visible){
		set(KEY_INDICATOR_VISIBLE, visible);
	}

	/** @return whether the indicator blinks for a while when a new error appears */
	public synchronized boolean isBlinkOnNewError(){
		return flag(KEY_INDICATOR_BLINK, true);
	}

	public void setBlinkOnNewError(final boolean blink){
		set(KEY_INDICATOR_BLINK, blink);
	}

	/** @return whether the palette opens again after the user acted on a finding and the findings changed */
	public synchronized boolean isReopenAfterAction(){
		return flag(KEY_REOPEN, true);
	}

	public void setReopenAfterAction(final boolean reopen){
		set(KEY_REOPEN, reopen);
	}

	/** @return whether the counts of what the user does with the findings are collected (off until the user turns it on) */
	public synchronized boolean isStatisticsEnabled(){
		return flag(KEY_STATISTICS, false);
	}

	public void setStatisticsEnabled(final boolean enabled){
		set(KEY_STATISTICS, enabled);
	}

	/** @return whether the assistant looks for findings when the palette is opened */
	public synchronized boolean isEnabled(final String assistantId){
		return !names(KEY_DISABLED).contains(assistantId);
	}

	/** @return whether the assistant looks for findings to count in the indicator (it has to be enabled for the palette too) */
	public synchronized boolean isEnabledForIndicator(final String assistantId){
		return isEnabled(assistantId) && !names(KEY_INDICATOR_DISABLED).contains(assistantId);
	}

	public void setEnabled(final String assistantId, final boolean enabled){
		change(KEY_DISABLED, assistantId, !enabled);
	}

	public void setEnabledForIndicator(final String assistantId, final boolean enabled){
		change(KEY_INDICATOR_DISABLED, assistantId, !enabled);
	}

	/** @return the identifiers of the assistants that are switched off for the indicator only (or for the palette too) */
	public synchronized Set<String> indicatorDisabled(){
		return names(KEY_INDICATOR_DISABLED);
	}

	/** @return the numbers the assistants use as conventions (the ages of the chronology, the weights of the suggestions) */
	public synchronized AssistantTuning tuning(){
		return AssistantTuning.read(key -> storage.get(key, null));
	}

	/** Keeps the numbers the assistants use as conventions. */
	public void setTuning(final AssistantTuning tuning){
		synchronized(this){
			if(tuning.equals(tuning()))
				return;

			for(final AssistantTuning.Parameter parameter : AssistantTuning.Parameter.values())
				storage.put(parameter.key(), tuning.text(parameter));
		}
		listeners.forEach(Runnable::run);
	}

	/** @param listener told every time a setting changes (it must be quick, and it may be called from any thread) */
	public void addListener(final Runnable listener){
		listeners.add(listener);
	}


	private boolean flag(final String key, final boolean defaultValue){
		final String value = storage.get(key, null);
		return (value == null || value.isBlank()? defaultValue: Boolean.parseBoolean(value.trim()));
	}

	private void set(final String key, final boolean value){
		synchronized(this){
			final String kept = storage.get(key, null);
			if(kept != null && !kept.isBlank() && Boolean.parseBoolean(kept.trim()) == value)
				return;

			storage.put(key, Boolean.toString(value));
		}
		listeners.forEach(Runnable::run);
	}

	private Set<String> names(final String key){
		final String value = storage.get(key, "");
		final Set<String> names = new LinkedHashSet<>();
		if(value != null && !value.isBlank())
			Arrays.stream(StringUtils.split(value, SEPARATOR))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.forEach(names::add);
		return names;
	}

	private void change(final String key, final String assistantId, final boolean add){
		synchronized(this){
			final Set<String> names = new TreeSet<>(names(key));
			if(!(add? names.add(assistantId): names.remove(assistantId)))
				return;

			storage.put(key, StringUtils.join(names, SEPARATOR));
		}
		listeners.forEach(Runnable::run);
	}

}
