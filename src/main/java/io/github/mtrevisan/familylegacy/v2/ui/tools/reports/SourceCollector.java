package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
final class SourceCollector{

	private static final String TYPE_SOURCE = "source";
	private static final String TYPE_RELATIONSHIP = "relationship";
	private static final String TAG_SUBJECT = "subject";
	private static final String TYPE_INDIVIDUAL = "individual";


	private SourceCollector(){
	}


	/** Returns the de-duplicated source IDs cited by the root individual. */
	static Set<String> collect(final ReportContext ctx){
		final Set<String> ids = new LinkedHashSet<>();

		collectFrom(ctx, ctx.root, ids);
		for(final FLEFRecord e : ctx.index.eventsOf(ctx.root))
			collectFrom(ctx, e, ids);
		for(final FLEFRecord a : ctx.index.attributesOf(ctx.root))
			collectFrom(ctx, a, ids);

		for(final FLEFRecord r : ctx.visibleRecordsByType(TYPE_RELATIONSHIP)){
			final String subj = r.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			if(Objects.equals(ctx.root.getId(), subj))
				collectFrom(ctx, r, ids);
		}

		return ids;
	}

	/** Appends source IDs into the given list (order preserved). */
	static void collect(final ReportContext ctx, final List<String> out){
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
	static Set<String> collectForGroup(final ReportContext ctx){
		final Set<String> ids = new LinkedHashSet<>();

		collectFrom(ctx, ctx.root, ids);
		for(final FLEFRecord e : ctx.index.eventsOfGroup(ctx.root))
			collectFrom(ctx, e, ids);
		for(final FLEFRecord a : ctx.index.attributesOfGroup(ctx.root))
			collectFrom(ctx, a, ids);
		for(final FLEFRecord m : ctx.index.membersOf(ctx.root))
			collectFrom(ctx, m, ids);
		for(final FLEFRecord r : ctx.index.parentGroupsOf(ctx.root))
			collectFrom(ctx, r, ids);
		for(final FLEFRecord r : ctx.index.childGroupsOf(ctx.root))
			collectFrom(ctx, r, ids);

		return ids;
	}

	/** Appends source IDs of a group into the given list (order preserved). */
	static void collectForGroup(final ReportContext ctx, final List<String> out){
		out.addAll(collectForGroup(ctx));
	}

}
