package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a repository report (root = {@code repository} record).
 *
 * <p>Renders:</p>
 * <ul>
 *   <li>primary and secondary names, with type and locale;</li>
 *   <li>custodian;</li>
 *   <li>place;</li>
 *   <li>contacts;</li>
 *   <li>every source stored in this repository, with locator and media type;</li>
 *   <li>notes, sources, audit and privacy of the repository itself.</li>
 * </ul>
 */
final class RepositoryRootSection implements SectionBuilder{

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_LOCALE = "locale";
	private static final String TAG_CUSTODIAN = "custodian";
	private static final String TAG_CONTACT = "contact";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_MEDIA_TYPE = "media_type";

	private static final String TYPE_REPOSITORY = "repository";


	private final ReportContext ctx;


	RepositoryRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isRepositoryRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.repositoryOf(),
			ReportFormatters.escape(primaryName(ctx.root)))));

		writeBasicInfo(out);
		writeNames(out);
		writeCustodian(out);
		writePlace(out);
		writeContacts(out);
		writeSources(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.type(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TYPE));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeNames(final List<ReportSection> out){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(ctx.root, TAG_NAME);
		if(names.size() <= 1)
			return;

		final FLEFRecord primary = primaryNameNode(ctx.root);
		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord n : names){
			if(n == primary) continue;
			final String type = FLEFRecordHelper.getChildValue(n, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			final String locale = FLEFRecordHelper.getChildValue(n, TAG_LOCALE);
			if(value == null || value.isBlank()) continue;
			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.name());
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


	private void writeCustodian(final List<ReportSection> out){
		final String custId = ctx.root.extractReferencedId(TAG_CUSTODIAN, "individual");
		if(custId == null)
			return;
		final FLEFRecord cust = ctx.visible(ctx.model.getRecordById(custId));
		if(cust == null)
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.repositoryCustodian() + ":** "
				+ ReportFormatters.escape(ctx.displayText(cust))));
	}


	private void writePlace(final List<ReportSection> out){
		final String place = FLEFRecordHelper.extractPlace(ctx.root, ctx.model);
		if(place == null)
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.place() + ":** " + ReportFormatters.escape(place)));
	}


	private void writeContacts(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord contact : ctx.visibleChildren(ctx.root, TAG_CONTACT)){
			final String rendered = ReportFormatters.renderContact(contact, ctx.labels);
			if(rendered != null)
				rows.add(rendered);
		}
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeSources(final List<ReportSection> out){
		final List<FLEFRecord> sources = ctx.index.sourcesOfRepository(ctx.root);
		if(sources.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.repositorySources()));
		for(final FLEFRecord src : sources){
			final String title = ctx.sourceTitle(src.getId());
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(title)));

			final List<String> rows = new ArrayList<>();
			ReportFormatters.appendIfPresent(rows, ctx.labels.sourceAuthor(),
				FLEFRecordHelper.getChildValue(src, "author"));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sourceDate(),
				FLEFRecordHelper.extractDate(src));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sourceMediaType(),
				FLEFRecordHelper.getChildValue(src, TAG_MEDIA_TYPE));

			// Locator(s) of this source within the repository.
			for(final FLEFRecord rc : ctx.index.repositoriesOfSource(src)){
				final String rid = rc.extractReferencedId(TYPE_REPOSITORY, TYPE_REPOSITORY);
				if(!ctx.root.getId().equals(rid))
					continue;
				final String locator = FLEFRecordHelper.getChildValue(rc, TAG_LOCATOR);
				if(locator != null && !locator.isBlank())
					rows.add("**" + ctx.labels.sourceLocator() + ":** "
						+ ReportFormatters.escape(locator));
			}

			if(!rows.isEmpty())
				out.add(new ReportSection.BulletList(rows));
		}
	}


	/* ----- Name helpers ---------------------------------------------------- */

	private static FLEFRecord primaryNameNode(final FLEFRecord repo){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(repo, TAG_NAME);
		if(names.isEmpty()) return null;
		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, TAG_TYPE);
			if("official".equalsIgnoreCase(type)){
				final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
				if(v != null && !v.isBlank()) return n;
			}
		}
		for(final FLEFRecord n : names){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank()) return n;
		}
		return null;
	}

	private static String primaryName(final FLEFRecord repo){
		final FLEFRecord n = primaryNameNode(repo);
		if(n != null){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank()) return v.trim();
		}
		return ReportFormatters.orEmpty(repo.getId());
	}

}
