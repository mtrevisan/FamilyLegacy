package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the document-information section (front matter).
 *
 * <p>Renders the fields of the {@code header} record:</p>
 * <ul>
 *   <li>protocol name and version;</li>
 *   <li>producing software (name, version, organization);</li>
 *   <li>creation date;</li>
 *   <li>copyright notice;</li>
 *   <li>submitter (contacts and note);</li>
 *   <li>scope of the file.</li>
 * </ul>
 *
 * <p>Emitted only when {@code config.header()} is enabled and the header
 * record contains at least one displayable field.</p>
 */
final class HeaderSection implements SectionBuilder{

	private static final String TAG_PROTOCOL = "protocol";
	private static final String TAG_NAME = "name";
	private static final String TAG_VERSION = "version";
	private static final String TAG_ORGANIZATION = "organization";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_DATE = "date";
	private static final String TAG_COPYRIGHT = "copyright";
	private static final String TAG_SUBMITTER = "submitter";
	private static final String TAG_CONTACT = "contact";
	private static final String TAG_NOTE = "note";
	private static final String TAG_SCOPE = "scope";


	private final ReportContext ctx;


	HeaderSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.header())
			return List.of();
		final FLEFRecord header = ctx.model.getHeader();
		if(header == null || !header.hasData())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.header()));

		appendProtocol(out, header);
		appendSource(out, header);
		appendAdministrative(out, header);
		appendSubmitter(out, header);
		appendScope(out, header);

		// If only the heading was produced, drop the whole section.
		if(out.size() == 1)
			return List.of();
		return out;
	}


	/* ----- Protocol -------------------------------------------------------- */

	private void appendProtocol(final List<ReportSection> out, final FLEFRecord header){
		final String name = FLEFRecordHelper.getChildValue(header,
			TAG_PROTOCOL + "." + TAG_NAME);
		final String version = FLEFRecordHelper.getChildValue(header,
			TAG_PROTOCOL + "." + TAG_VERSION);
		if(name == null && version == null)
			return;

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerProtocol(), name);
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerProtocolVersion(), version);
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Source ---------------------------------------------------------- */

	private void appendSource(final List<ReportSection> out, final FLEFRecord header){
		final String name = FLEFRecordHelper.getChildValue(header,
			TAG_SOURCE + "." + TAG_NAME);
		final String version = FLEFRecordHelper.getChildValue(header,
			TAG_SOURCE + "." + TAG_VERSION);
		final String organization = FLEFRecordHelper.getChildValue(header,
			TAG_SOURCE + "." + TAG_ORGANIZATION);
		if(name == null && version == null && organization == null)
			return;

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerSource(), name);
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerSourceVersion(), version);
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerOrganization(), organization);
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Date / copyright ------------------------------------------------ */

	private void appendAdministrative(final List<ReportSection> out, final FLEFRecord header){
		final String date = FLEFRecordHelper.getChildValue(header, TAG_DATE);
		final String copyright = FLEFRecordHelper.getChildValue(header, TAG_COPYRIGHT);
		if(date == null && copyright == null)
			return;

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerDate(), date);
		ReportFormatters.appendIfPresent(rows, ctx.labels.headerCopyright(), copyright);
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Submitter ------------------------------------------------------- */

	private void appendSubmitter(final List<ReportSection> out, final FLEFRecord header){
		final FLEFRecord submitter = FLEFRecordHelper.findChild(header, TAG_SUBMITTER);
		if(submitter == null)
			return;

		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord contact : FLEFRecordHelper.findChildren(submitter, TAG_CONTACT)){
			final String rendered = ReportFormatters.renderContact(contact, ctx.labels);
			if(rendered != null)
				rows.add(rendered);
		}
		final String note = FLEFRecordHelper.getChildValue(submitter, TAG_NOTE);
		if(note != null && !note.isBlank())
			rows.add("**" + ctx.labels.headerSubmitterNote() + ":** "
				+ ReportFormatters.escape(note));

		if(rows.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.headerSubmitter()));
		out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Scope ----------------------------------------------------------- */

	private void appendScope(final List<ReportSection> out, final FLEFRecord header){
		final String scope = FLEFRecordHelper.getChildValue(header, TAG_SCOPE);
		if(scope == null || scope.isBlank())
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.headerScope() + ":** " + ReportFormatters.escape(scope)));
	}

}
