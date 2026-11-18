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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Collects the IDs of every {@code source} record cited by the root individual
 * or by any of its events, attributes and relationships.
 *
 * <p>Covers both sides of the privacy policy:</p>
 * <ul>
 *   <li>citations with {@code privacy.level = confidential} are skipped;</li>
 *   <li>citations pointing to a source with {@code privacy.level = confidential}
 *       are skipped;</li>
 *   <li>relationships with {@code privacy.level = confidential} are skipped
 *       entirely.</li>
 * </ul>
 */
public final class SourceCollector{

	private static final String TYPE_SOURCE = SourceCitationReader.TAG_SOURCE;
	private static final String TYPE_RELATIONSHIP = RelationshipReader.TYPE;
	private static final String TAG_SUBJECT = RelationshipReader.TAG_SUBJECT;


	private SourceCollector(){
	}


	/** Returns the de-duplicated source IDs cited by the root individual (by every person of a multi-person report). */
	public static Set<String> collect(final ReportContext ctx){
		final Set<String> ids = new LinkedHashSet<>();

		final Set<String> subjectIds = new HashSet<>();
		for(final FLEFRecord subject : ctx.subjects()){
			//(a group has its own events, attributes, members and groups)
			if(ReportContext.rootKindOf(subject) == ReportContext.RootKind.GROUP){
				collectOfGroup(ctx, subject, ids);
				continue;
			}

			subjectIds.add(subject.getId());

			collectFrom(ctx, subject, ids);
			for(final FLEFRecord e : ctx.index.eventsOf(subject))
				collectFrom(ctx, e, ids);
			for(final FLEFRecord a : ctx.index.attributesOf(subject))
				collectFrom(ctx, a, ids);
		}

		for(final FLEFRecord r : ctx.visibleRecordsByType(TYPE_RELATIONSHIP)){
			final String subj = r.extractReferencedId(TAG_SUBJECT, IndividualReader.TYPE);
			if(subj != null && subjectIds.contains(subj))
				collectFrom(ctx, r, ids);
		}

		return ids;
	}

	/** Appends source IDs into the given list (order preserved). */
	public static void collect(final ReportContext ctx, final List<String> out){
		out.addAll(collect(ctx));
	}


	private static void collectFrom(final ReportContext ctx, final FLEFRecord rec,
		final Set<String> out){
		for(final FLEFRecord cit : ctx.visibleChildren(rec, TYPE_SOURCE)){
			final String sid = ReportFormatters.extractSourceId(cit);
			if(sid == null)
				continue;
			final FLEFRecord src = ctx.model.getRecordById(sid);
			if(ctx.isVisible(src))
				out.add(sid);
		}
	}

	/** Returns the de-duplicated source IDs cited by the group. */
	public static Set<String> collectForGroup(final ReportContext ctx){
		final Set<String> ids = new LinkedHashSet<>();

		collectOfGroup(ctx, ctx.root, ids);

		return ids;
	}

	private static void collectOfGroup(final ReportContext ctx, final FLEFRecord group, final Set<String> ids){
		collectFrom(ctx, group, ids);
		for(final FLEFRecord e : ctx.index.eventsOfGroup(group))
			collectFrom(ctx, e, ids);
		for(final FLEFRecord a : ctx.index.attributesOfGroup(group))
			collectFrom(ctx, a, ids);
		for(final FLEFRecord m : ctx.index.membersOf(group))
			collectFrom(ctx, m, ids);
		for(final FLEFRecord r : ctx.index.parentGroupsOf(group))
			collectFrom(ctx, r, ids);
		for(final FLEFRecord r : ctx.index.childGroupsOf(group))
			collectFrom(ctx, r, ids);
	}

	/** Appends source IDs of a group into the given list (order preserved). */
	public static void collectForGroup(final ReportContext ctx, final List<String> out){
		out.addAll(collectForGroup(ctx));
	}

}
