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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;


/**
 * How much each source and repository is used: how many citations a source has, how many sources a repository holds. The lists of the
 * Source menu show it on each line (a source with no citation is the one to look at).
 * <p>
 * The counts need the whole model, so they are indexed once and reused for a short time: describing a thousand sources in a row costs
 * one scan of the model. The model has no change notification, so the index expires after {@link #CACHE_MILLIS} and when the number of
 * records changes.
 */
public final class SourceUsage{

	private static final long CACHE_MILLIS = 2_000L;


	private record Index(WeakReference<FLEFModel> model, int recordCount, long builtAt, Map<String, Integer> citations,
		Map<String, Integer> sources){}


	private static volatile Index cache;


	private SourceUsage(){}


	/** Drops the index, so that the next description reads the model again. */
	public static void invalidate(){
		cache = null;
	}

	/** @return	"3 citations". */
	public static String citations(final FLEFRecord source, final FLEFModel model){
		return I18N.tf("text.usage.citations", indexOf(model).citations().getOrDefault(source.getId(), 0));
	}

	/** @return	"2 sources". */
	public static String sources(final FLEFRecord repository, final FLEFModel model){
		return I18N.tf("text.usage.sources", indexOf(model).sources().getOrDefault(repository.getId(), 0));
	}


	private static Index indexOf(final FLEFModel model){
		final long now = System.currentTimeMillis();
		final Index current = cache;
		if(current != null && current.model().get() == model && current.recordCount() == model.getRecordCount()
				&& now - current.builtAt() < CACHE_MILLIS)
			return current;

		final Map<String, Integer> sources = new HashMap<>();
		for(final FLEFRecord source : SourceHelper.listAllSources(model))
			for(final FLEFRecord child : source.getChildren())
				if(RepositoryReader.isOfType(child)){
					final String repositoryId = FLEFRecordHelper.getChildValue(child, RepositoryReader.TYPE);
					if(repositoryId != null){
						sources.merge(repositoryId, 1, Integer::sum);
						break;
					}
				}

		final Index index = new Index(new WeakReference<>(model), model.getRecordCount(), now, SourceHelper.countCitationsPerSource(model), sources);
		cache = index;
		return index;
	}

}
