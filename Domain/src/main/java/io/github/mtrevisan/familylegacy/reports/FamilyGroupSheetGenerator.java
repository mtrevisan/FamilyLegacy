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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.EventIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Generates the family group sheets of an individual: for each of their partners (and for the children whose other parent is not
 * known) a page with the two parents, the marriage and the children, each with the dates and the places of birth and death.
 * <p>
 * An individual the privacy policy shows with the name and the role only has no dates and places.
 * </p>
 */
public final class FamilyGroupSheetGenerator{

	private static final String NO_VALUE = "";

	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public FamilyGroupSheetGenerator(final FLEFModel model, final FLEFRecord root, final ReportConfig config){
		this.model = Objects.requireNonNull(model, "model");
		this.root = Objects.requireNonNull(root, "root");
		this.config = Objects.requireNonNull(config, "config");
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportContext ctx = ReportContext.build(model, root, config);
		final ReportLabels labels = ctx.labels;
		final String title = labels.sections().familyGroupTitle();
		final String language = labels.language().locale().toLanguageTag();
		final TreeText text = new TreeText(ctx);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		listener.onProgress(10, null);

		//the children by the other parent (null if it is not known), the partners without children after the ones that have them
		final Map<String, List<FLEFRecord>> childrenByPartner = new LinkedHashMap<>();
		for(final FLEFRecord child : text.childrenOf(root)){
			final FLEFRecord other = ctx.index.otherParentOf(child, root);
			childrenByPartner.computeIfAbsent((other != null? other.getId(): null), id -> new ArrayList<>())
				.add(child);
		}
		final List<FLEFRecord> partners = new ArrayList<>();
		for(final FLEFRecord spouse : ctx.index.spousesOf(root))
			if(partners.stream().noneMatch(p -> p.getId().equals(spouse.getId())))
				partners.add(spouse);
		for(final String id : childrenByPartner.keySet())
			if(id != null && partners.stream().noneMatch(p -> p.getId().equals(id)))
				partners.add(model.getRecordById(id));

		int done = 0;
		for(final FLEFRecord partner : partners){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			sections.addAll(sheet(ctx, text, partner, childrenByPartner.getOrDefault(partner.getId(), List.of())));
			done ++;
			listener.onProgress(10 + 85 * done / (partners.size() + 1), null);
		}
		final List<FLEFRecord> alone = childrenByPartner.get(null);
		if(alone != null || partners.isEmpty())
			sections.addAll(sheet(ctx, text, null, (alone != null? alone: List.of())));
		listener.onProgress(100, null);

		return new ReportDocument(title, ReportFormatters.escape(ctx.displayText(root)), sections, language, labels.sections().contents());
	}


	/** @return	The sheet of the family of the root and the partner ({@code null} if the other parent of the children is not known). */
	private List<ReportSection> sheet(final ReportContext ctx, final TreeText text, final FLEFRecord partner, final List<FLEFRecord> children){
		final ReportLabels labels = ctx.labels;
		final List<ReportSection> out = new ArrayList<>();

		final String heading = ReportFormatters.escape(ctx.displayText(root))
			+ (partner != null? " – " + ReportFormatters.escape(ctx.displayText(partner)): " – " + labels.sections().familyGroupNoPartner());
		out.add(new ReportSection.Heading(1, heading));

		final List<List<String>> parents = new ArrayList<>();
		parents.add(row(ctx, root, null));
		if(partner != null)
			parents.add(row(ctx, partner, null));
		out.add(new ReportSection.Heading(2, labels.sections().familyGroupParents()));
		out.add(new ReportSection.Table(columns(labels, false), parents));

		if(partner != null){
			final String marriage = marriage(ctx, partner);
			if(marriage != null){
				out.add(new ReportSection.Heading(2, labels.sections().familyGroupMarriage()));
				out.add(new ReportSection.Paragraph(marriage));
			}
		}

		out.add(new ReportSection.Heading(2, labels.sections().familyGroupChildren()));
		if(children.isEmpty())
			out.add(new ReportSection.Paragraph(labels.sections().familyGroupNoChildren()));
		else{
			final List<List<String>> rows = new ArrayList<>(children.size());
			int number = 1;
			for(final FLEFRecord child : children)
				rows.add(row(ctx, child, number ++));
			out.add(new ReportSection.Table(columns(labels, true), rows));
		}
		return out;
	}

	private static List<String> columns(final ReportLabels labels, final boolean numbered){
		final List<String> columns = new ArrayList<>();
		if(numbered)
			columns.add("#");
		columns.add(labels.sections().familyGroupColumn("NAME"));
		columns.add(labels.sections().familyGroupColumn("BORN"));
		columns.add(labels.sections().familyGroupColumn("BIRTH_PLACE"));
		columns.add(labels.sections().familyGroupColumn("DIED"));
		columns.add(labels.sections().familyGroupColumn("DEATH_PLACE"));
		return columns;
	}

	private List<String> row(final ReportContext ctx, final FLEFRecord person, final Integer number){
		final List<String> row = new ArrayList<>();
		if(number != null)
			row.add(number.toString());
		row.add(ReportFormatters.escape(ctx.displayText(person)));

		//a person shown with the name and the role only has nothing else
		final FLEFRecord birth = (ctx.isRedacted(person)? null: event(ctx, person, EventReader.ENUM_TYPE_BIRTH));
		final FLEFRecord death = (ctx.isRedacted(person)? null: event(ctx, person, EventReader.ENUM_TYPE_DEATH));
		row.add(date(ctx, birth));
		row.add(place(birth));
		row.add(date(ctx, death));
		row.add(place(death));
		return row;
	}

	private static FLEFRecord event(final ReportContext ctx, final FLEFRecord person, final String type){
		for(final FLEFRecord event : ctx.index.eventsOf(person))
			if(type.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
				return event;
		return null;
	}

	private static String date(final ReportContext ctx, final FLEFRecord event){
		final String date = (event != null? ctx.labels.dateText(event): null);
		return (StringUtils.isNotBlank(date)? ReportFormatters.escape(date): NO_VALUE);
	}

	private String place(final FLEFRecord event){
		final String place = (event != null? PlaceCitationReader.extractPlace(event, model): null);
		return (StringUtils.isNotBlank(place)? ReportFormatters.escape(place): NO_VALUE);
	}

	/** @return	The date and the place of the marriage of the root and the partner, {@code null} if there is no such event. */
	private String marriage(final ReportContext ctx, final FLEFRecord partner){
		if(ctx.isRedacted(root) || ctx.isRedacted(partner))
			return null;

		for(final FLEFRecord event : ctx.index.eventsOf(root)){
			if(!EventReader.ENUM_TYPE_MARRIAGE.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
				continue;

			boolean shared = false;
			for(final EventIndex.Participant participant : ctx.index.participantsOf(event))
				if(participant.record() != null && Objects.equals(participant.record().getId(), partner.getId())){
					shared = true;
					break;
				}
			if(!shared)
				continue;

			final String date = date(ctx, event);
			final String place = place(event);
			if(date.isEmpty() && place.isEmpty())
				return null;
			return (date.isEmpty()? place: (place.isEmpty()? date: date + ", " + place));
		}
		return null;
	}

}
