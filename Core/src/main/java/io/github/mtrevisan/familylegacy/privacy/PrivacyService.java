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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.preferences.AppPreferences;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * The application-wide privacy policy and the filter built on it.
 * <p>
 * The policy ({@link PrivacyPolicy#SHOW_ALL} by default) is persisted in {@link AppPreferences} and decides which records
 * the views and the searches hide; the level of each record is the <em>effective</em> one computed by
 * {@link PrivacyResolver}. Editors are never filtered.
 * <p>
 * The resolver is cached per model. The model does not notify its changes, so the cache is dropped when the model object or
 * its number of records changes, and must be dropped with {@link #invalidate()} after an edit that does not change the
 * number of records (the record dialogs do it when they save).
 */
public final class PrivacyService{

	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

	private static PrivacyPolicy policy = persistedPolicy();
	private static FLEFModel cachedModel;
	private static int cachedRecordCount;
	private static PrivacyResolver cachedResolver;


	private PrivacyService(){}


	/** @return the policy persisted in the preferences, or {@link PrivacyPolicy#SHOW_ALL} when unset or unknown */
	private static PrivacyPolicy persistedPolicy(){
		try{
			final String name = AppPreferences.getInstance()
				.privacyPolicyName();
			return (name != null? PrivacyPolicy.valueOf(name): PrivacyPolicy.SHOW_ALL);
		}
		catch(final IllegalArgumentException ignored){
			return PrivacyPolicy.SHOW_ALL;
		}
	}


	public static synchronized PrivacyPolicy policy(){
		return policy;
	}

	/** Changes the policy, persists it, and notifies the listeners so that the views can refresh. */
	public static void setPolicy(final PrivacyPolicy newPolicy){
		Objects.requireNonNull(newPolicy, "newPolicy");

		synchronized(PrivacyService.class){
			if(newPolicy == policy)
				return;

			policy = newPolicy;
		}
		AppPreferences.getInstance()
			.setPrivacyPolicyName(newPolicy.name());
		fireChanged();
	}

	/** Registers a listener called when the policy changes or the cache has been invalidated. */
	public static void addListener(final Runnable listener){
		LISTENERS.add(Objects.requireNonNull(listener, "listener"));
	}

	public static void removeListener(final Runnable listener){
		LISTENERS.remove(listener);
	}

	/** Drops the cached levels: to be called after an edit of a record. */
	public static synchronized void invalidate(){
		cachedResolver = null;
	}

	private static void fireChanged(){
		for(final Runnable listener : LISTENERS)
			listener.run();
	}


	/** The resolver of the model, cached while the model does not change. */
	public static synchronized PrivacyResolver resolver(final FLEFModel model){
		if(cachedResolver == null || model != cachedModel || model.getRecordCount() != cachedRecordCount){
			cachedResolver = new PrivacyResolver(model);
			cachedModel = model;
			cachedRecordCount = model.getRecordCount();
		}
		return cachedResolver;
	}

	/** Whether the record is shown under the current policy. A {@code null} record is not visible. */
	public static boolean isVisible(final FLEFModel model, final FLEFRecord record){
		return (record != null && !policy().hides(resolver(model).effectiveLevel(record)));
	}

	/** Whether the record is hidden under the current policy; a {@code null} record is not hidden. */
	public static boolean isHidden(final FLEFModel model, final FLEFRecord record){
		return (record != null && !isVisible(model, record));
	}

	/** The records that are shown under the current policy, in the same order. */
	public static List<FLEFRecord> visible(final FLEFModel model, final List<FLEFRecord> records){
		if(policy() == PrivacyPolicy.SHOW_ALL)
			return records;

		return records.stream()
			.filter(record -> isVisible(model, record))
			.toList();
	}

}
