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

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the coverage report.
 *
 * <p>For each record attached to the root, the section reports how many of
 * its expected fields are populated, whether it carries a source citation,
 * and whether it carries an evidence qualifier. This is a diagnostic tool:
 * it does not describe the subject, it describes the state of the research.</p>
 *
 * <p>The report is emitted only when {@code config.coverageReport()} is
 * enabled.</p>
 */
public final class CoverageSection implements SectionBuilder{

	private static final String TAG_EVIDENCE = EvidenceQualifiersReader.TAG_EVIDENCE;
	private static final String TAG_DATE = EventReader.TAG_DATE;
	private static final String TAG_PLACE = EventReader.TAG_PLACE;


	private final ReportContext ctx;


	public CoverageSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.coverageReport())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().coverageReport()));

		final List<CoverageEntry> entries = collectEntries();
		if(entries.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
			return out;
		}

		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().coverageRecord(),
				ctx.labels.sections().coverageFields(),
				ctx.labels.sections().coverageSources(),
				ctx.labels.sections().coverageEvidence()),
			buildRows(entries)));

		// Summary line: how many records lack a source.
		final long missingSources = entries.stream()
			.filter(e -> !e.hasSource)
			.count();
		if(missingSources > 0){
			out.add(new ReportSection.Paragraph(
				"*" + ctx.labels.sections().coverageMissingSources() + ":* " + missingSources));
		}
		return out;
	}


	private List<CoverageEntry> collectEntries(){
		final List<CoverageEntry> out = new ArrayList<>();

		switch(ctx.rootKind()){
			case INDIVIDUAL -> {
				out.add(coverageOf(ctx.root, kindLabel(IndividualReader.TYPE), IndividualReader.TAG_NAME, IndividualReader.TAG_SEX));
				for(final FLEFRecord e : ctx.index.eventsOf(ctx.root))
					out.add(coverageOf(e, kindLabel(EventReader.TYPE), EventReader.TAG_TYPE, EventReader.TAG_DATE, EventReader.TAG_PLACE));
				for(final FLEFRecord a : ctx.index.attributesOf(ctx.root))
					out.add(coverageOf(a, kindLabel(IndividualAttributeReader.TYPE), IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.TAG_VALUE));
				for(final FLEFRecord r : ctx.index.relationshipsOfSubject(ctx.root))
					out.add(coverageOf(r, kindLabel(RelationshipReader.TYPE), RelationshipReader.TAG_TYPE, RelationshipReader.TAG_SUBJECT,
						RelationshipReader.TAG_TARGET));
			}
			case GROUP -> {
				out.add(coverageOf(ctx.root, kindLabel(GroupReader.TYPE), GroupReader.TAG_NAME, GroupReader.TAG_TYPE));
				for(final FLEFRecord a : ctx.index.attributesOfGroup(ctx.root))
					out.add(coverageOf(a, kindLabel(GroupAttributeReader.TYPE), GroupAttributeReader.TAG_TYPE, GroupAttributeReader.TAG_VALUE));
				for(final FLEFRecord e : ctx.index.eventsOfGroup(ctx.root))
					out.add(coverageOf(e, kindLabel(EventReader.TYPE), EventReader.TAG_TYPE, EventReader.TAG_DATE, EventReader.TAG_PLACE));
			}
			case EVENT -> out.add(coverageOf(ctx.root, kindLabel(EventReader.TYPE),
				EventReader.TAG_TYPE, EventReader.TAG_DATE, EventReader.TAG_PLACE));
			case SOURCE -> out.add(coverageOf(ctx.root, kindLabel(SourceReader.TYPE),
				SourceReader.TAG_TITLE, SourceReader.TAG_AUTHOR, SourceReader.TAG_PUBLISHER, SourceReader.TAG_DATE, SourceReader.TAG_REPOSITORY));
			case PLACE -> out.add(coverageOf(ctx.root, kindLabel(PlaceReader.TYPE), PlaceReader.TAG_NAME, PlaceReader.TAG_TYPE));
			case OTHER -> { /* no coverage report */ }
		}
		return out;
	}

	private String kindLabel(final String type){
		return ctx.labels.sections().coverageKind(type);
	}

	private CoverageEntry coverageOf(final FLEFRecord rec, final String kind,
		final String... expectedFields){
		int present = 0;
		for(final String field : expectedFields)
			if(isPopulated(rec, field))
				present ++;
		final boolean hasSource = SourceCitationReader.hasSupportingCitation(rec);
		final boolean hasEvidence = hasChild(rec, TAG_EVIDENCE);
		return new CoverageEntry(rec, kind, present, expectedFields.length, hasSource, hasEvidence);
	}

	private static boolean isPopulated(final FLEFRecord rec, final String field){
		if(EventReader.TAG_TYPE.equals(field) || IndividualAttributeReader.TAG_VALUE.equals(field))
			return FLEFRecordHelper.findChildren(rec, field).stream().anyMatch(child -> child.getValue() != null);
		if(TAG_DATE.equals(field))
			return hasChild(rec, TAG_DATE);
		if(TAG_PLACE.equals(field))
			return hasChild(rec, TAG_PLACE);
		return hasChild(rec, field);
	}

	/** A record may repeat a tag (several names, several evidences): it counts as populated when there is at least one. */
	private static boolean hasChild(final FLEFRecord rec, final String path){
		return !FLEFRecordHelper.findChildren(rec, path).isEmpty();
	}

	private List<List<String>> buildRows(final List<CoverageEntry> entries){
		final List<List<String>> rows = new ArrayList<>(entries.size());
		for(final CoverageEntry e : entries){
			final String label = ReportFormatters.escape(ctx.displayText(e.record))
				+ " [" + e.kind + "]";
			final String fields = e.present + "/" + e.total;
			final String sources = (e.hasSource? "✓": "—");
			final String evidence = (e.hasEvidence? "✓": "—");
			rows.add(List.of(label, fields, sources, evidence));
		}
		return rows;
	}


	private static final class CoverageEntry{
		final FLEFRecord record;
		final String kind;
		final int present;
		final int total;
		final boolean hasSource;
		final boolean hasEvidence;

		CoverageEntry(final FLEFRecord record, final String kind, final int present,
			final int total, final boolean hasSource, final boolean hasEvidence){
			this.record = record;
			this.kind = kind;
			this.present = present;
			this.total = total;
			this.hasSource = hasSource;
			this.hasEvidence = hasEvidence;
		}
	}

}
