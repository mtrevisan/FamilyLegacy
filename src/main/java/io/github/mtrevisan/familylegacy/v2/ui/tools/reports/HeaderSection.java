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
		out.add(new ReportSection.Heading(1, ctx.labels.sections().header()));

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
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerProtocol(), name);
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerProtocolVersion(), version);
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
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerSource(), name);
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerSourceVersion(), version);
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerOrganization(), organization);
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
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerDate(), date);
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().headerCopyright(), copyright);
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
			final String rendered = ReportFormatters.renderContact(contact);
			if(rendered != null)
				rows.add(rendered);
		}
		final String note = FLEFRecordHelper.getChildValue(submitter, TAG_NOTE);
		if(note != null && !note.isBlank())
			rows.add("**" + ctx.labels.sections().headerSubmitterNote() + ":** "
				+ ReportFormatters.escape(note));

		if(rows.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().headerSubmitter()));
		out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Scope ----------------------------------------------------------- */

	private void appendScope(final List<ReportSection> out, final FLEFRecord header){
		final String scope = FLEFRecordHelper.getChildValue(header, TAG_SCOPE);
		if(scope == null || scope.isBlank())
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().headerScope() + ":** " + ReportFormatters.escape(scope)));
	}

}
