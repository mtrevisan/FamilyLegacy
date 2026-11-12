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
package io.github.mtrevisan.familylegacy.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * The important information that an import discarded (real losses only), collected while the file is read.
 * <p>
 * Thread-safe: the importer fills it away from the event dispatch thread, the dialog reads it afterwards. At most {@link #MAX_ISSUES}
 * issues are kept in detail; the others are only counted (see {@link #overflow()}).
 */
public final class ImportReport{

	/** The maximum number of issues kept in detail. */
	public static final int MAX_ISSUES = 5000;


	private final List<ImportIssue> issues = new ArrayList<>();
	private final Map<String, Integer> counts = new LinkedHashMap<>();
	private int overflow;
	private final List<PendingFile> pendingFiles = new ArrayList<>();


	/**
	 * Records a discarded item.
	 *
	 * @param reasonKey  the I18N key of the reason
	 * @param item       what was lost
	 * @param where      where it was found (may be {@code null})
	 */
	public synchronized void discard(final String reasonKey, final String item, final String where){
		counts.merge(reasonKey, 1, Integer::sum);
		if(issues.size() < MAX_ISSUES)
			issues.add(new ImportIssue(reasonKey, (item != null? item: ""), (where != null? where: "")));
		else
			overflow ++;
	}

	/**
	 * Records a file embedded in the imported data, to be written to disk by the application.
	 * <p>
	 * A file for a media that already has one is ignored.
	 */
	public synchronized void addPendingFile(final PendingFile file){
		if(file != null && pendingFiles.stream().noneMatch(pending -> pending.mediaId().equals(file.mediaId())))
			pendingFiles.add(file);
	}

	/** @return	The files to write to disk (a snapshot, unmodifiable). */
	public synchronized List<PendingFile> pendingFiles(){
		return List.copyOf(pendingFiles);
	}

	/** Forgets the pending files (once written, or declined). */
	public synchronized void clearPendingFiles(){
		pendingFiles.clear();
	}

	public synchronized boolean isEmpty(){
		return counts.isEmpty();
	}

	/** @return	The issues kept in detail, in the order they were found (a snapshot, unmodifiable). */
	public synchronized List<ImportIssue> issues(){
		return List.copyOf(issues);
	}

	/** @return	How many issues there are for each reason (all of them, also the ones not kept in detail), in order of first appearance. */
	public synchronized Map<String, Integer> countsByReason(){
		return new LinkedHashMap<>(counts);
	}

	/** @return	How many issues were counted but not kept in detail, because of the cap. */
	public synchronized int overflow(){
		return overflow;
	}

}
