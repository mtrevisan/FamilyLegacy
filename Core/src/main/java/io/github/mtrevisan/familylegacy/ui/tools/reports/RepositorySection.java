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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Builds the Repositories section.
 *
 * <p>For every source cited by the root (directly, or through its events,
 * attributes, relationships or participations), the section collects the
 * referenced {@code repository} records and renders each with:</p>
 * <ul>
 *   <li>its names, with type and locale;</li>
 *   <li>the custodian (an individual reference);</li>
 *   <li>its place;</li>
 *   <li>its contacts (email, phone, website, ...);</li>
 *   <li>its notes, sources, privacy and audit.</li>
 * </ul>
 *
 * <p>The section is emitted only when {@code config.repositories()} is
 * enabled and at least one repository is reachable.</p>
 */
final class RepositorySection implements SectionBuilder{

//	private static final String TAG_NAME = "name";
//	private static final String TAG_VALUE = "value";
//	private static final String TAG_TYPE = "type";
//	private static final String TAG_LOCALE = "locale";
//	private static final String TAG_CUSTODIAN = "custodian";
//	private static final String TAG_CONTACT = "contact";

	private static final String TYPE_REPOSITORY = "repository";
	private static final String NAME_TYPE_OFFICIAL = "official";


	private final ReportContext ctx;


	RepositorySection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.repositories())
			return List.of();

		final List<FLEFRecord> repos = collectRepositories();
		if(repos.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().repositories()));
		for(final FLEFRecord repo : repos)
			appendRepository(out, repo);
		return out;
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private List<FLEFRecord> collectRepositories(){
		final Set<String> ids = new LinkedHashSet<>();

		// Every source cited anywhere in the report.
		for(final String sid : SourceCollector.collect(ctx))
			collectFromSource(sid, ids);

		// Sources cited by the root's relationships too (SourceCollector
		// already covers individual/group roots, but relationships are not
		// part of the default traversal for group roots).
		for(final FLEFRecord r : ctx.visibleRecordsByType("relationship"))
			for(final FLEFRecord rc : FLEFRecordHelper.findChildren(r, "source"))
				collectFromSource(ReportFormatters.extractSourceId(rc), ids);

		final List<FLEFRecord> out = new ArrayList<>(ids.size());
		for(final String id : ids){
			final FLEFRecord repo = ctx.visible(ctx.model.getRecordById(id));
			if(repo != null)
				out.add(repo);
		}
		return out;
	}

	private void collectFromSource(final String sourceId, final Set<String> out){
		if(sourceId == null)
			return;
		final FLEFRecord src = ctx.model.getRecordById(sourceId);
		if(src == null)
			return;
		for(final FLEFRecord rc : FLEFRecordHelper.findChildren(src, TYPE_REPOSITORY)){
			final String rid = rc.extractReferencedId(TYPE_REPOSITORY, TYPE_REPOSITORY);
			if(rid != null)
				out.add(rid);
		}
	}


	/* ======================================================================
	 *                          Rendering
	 * ====================================================================== */

	private void appendRepository(final List<ReportSection> out, final FLEFRecord repo){
		out.add(new ReportSection.Heading(2, ReportFormatters.escape(
			primaryName(repo))));

		appendNames(out, repo);
		appendCustodian(out, repo);
		appendPlace(out, repo);
		appendContacts(out, repo);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(repo));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(repo));
		out.addAll(ctx.citations().audit(repo));
	}


	private void appendNames(final List<ReportSection> out, final FLEFRecord repo){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(repo, RepositoryReader.TAG_NAME);
		if(names.size() <= 1)
			return;

		final FLEFRecord primary = primaryNameNode(repo);
		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord n : names){
			if(n == primary)
				continue;
			final String type = FLEFRecordHelper.getChildValue(n, NameReader.TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			final String locale = FLEFRecordHelper.getChildValue(n, NameReader.TAG_LOCALE);
			if(value == null || value.isBlank())
				continue;
			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.sections().name());
			if(type != null && !type.isBlank())
				line.append(" (").append(ReportFormatters.escape(type)).append(")");
			line.append(":** ").append(ReportFormatters.escape(value));
			if(locale != null && !locale.isBlank())
				line.append(" — *").append(ReportFormatters.escape(locale)).append('*');
			rows.add(line.toString());
		}
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void appendCustodian(final List<ReportSection> out, final FLEFRecord repo){
		final String custId = repo.extractReferencedId(RepositoryReader.TAG_CUSTODIAN, IndividualHandler.TYPE);
		if(custId == null)
			return;
		final FLEFRecord cust = ctx.visible(ctx.model.getRecordById(custId));
		if(cust == null)
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().repositoryCustodian() + ":** "
				+ ReportFormatters.escape(ctx.displayText(cust))));
	}


	private void appendPlace(final List<ReportSection> out, final FLEFRecord repo){
		final String place = FLEFRecordHelper.extractPlace(repo, ctx.model);
		if(place == null)
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().place() + ":** " + ReportFormatters.escape(place)));
	}


	private void appendContacts(final List<ReportSection> out, final FLEFRecord repo){
		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord contact : ctx.visibleChildren(repo, RepositoryReader.TAG_CONTACT)){
			final String rendered = ReportFormatters.renderContact(contact);
			if(rendered != null)
				rows.add(rendered);
		}
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ======================================================================
	 *                          Name helpers
	 * ====================================================================== */

	private static FLEFRecord primaryNameNode(final FLEFRecord repo){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(repo, RepositoryReader.TAG_NAME);
		if(names.isEmpty())
			return null;
		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, NameReader.TAG_TYPE);
			if(NAME_TYPE_OFFICIAL.equalsIgnoreCase(type)){
				final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
				if(v != null && !v.isBlank())
					return n;
			}
		}
		for(final FLEFRecord n : names){
			final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank())
				return n;
		}
		return null;
	}

	private static String primaryName(final FLEFRecord repo){
		final FLEFRecord n = primaryNameNode(repo);
		if(n != null){
			final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank())
				return v.trim();
		}
		return ReportFormatters.orEmpty(repo.getId());
	}

}
