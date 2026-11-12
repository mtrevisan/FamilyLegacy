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
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.NamePart;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.reports.ReportDocument;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;


/**
 * Generates the Family Book: a document about everybody in the file, a page for each person (the introduction and the life story),
 * then a page for each group (if {@link ReportConfig#groups()} is on), then what all of them cite together (the repositories, sources,
 * media, places and the index of the places).
 * <p>
 * The persons are put in the order of {@link ReportConfig#personOrder()}, the groups by name; the ones the privacy policy hides are
 * left out.
 * </p>
 */
public final class FamilyBookGenerator{

	private final FLEFModel model;
	private final ReportConfig config;


	public FamilyBookGenerator(final FLEFModel model, final ReportConfig config){
		this.model = model;
		this.config = config;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language());
		final String title = labels.sections().familyBookTitle();

		final List<FLEFRecord> everybody = model.getRecordsByType(IndividualReader.TYPE);
		final List<FLEFRecord> allGroups = (config.groups()? model.getRecordsByType(GroupReader.TYPE): List.of());
		final List<FLEFRecord> persons = new ArrayList<>();
		final List<FLEFRecord> groups = new ArrayList<>();
		if(!everybody.isEmpty() || !allGroups.isEmpty()){
			final ReportContext visibility = ReportContext.build(model, (everybody.isEmpty()? allGroups.getFirst(): everybody.getFirst()), config);
			for(final FLEFRecord person : everybody)
				if(visibility.isVisible(person))
					persons.add(person);
			for(final FLEFRecord group : allGroups)
				if(visibility.isVisible(group))
					groups.add(group);
		}

		final RelationIndex index = RelationIndex.build(model, r -> true);
		final Locale locale = labels.language().locale();
		final List<FLEFRecord> ordered = order(persons, index, locale);
		groups.sort(Comparator.comparing((final FLEFRecord group) -> RecordDisplay.describe(group, model).toLowerCase(locale))
			.thenComparing(FLEFRecord::getId));
		return new MultiPersonReportGenerator(model, ordered, groups, config, title, List.of()).generate(listener);
	}


	private List<FLEFRecord> order(final List<FLEFRecord> persons, final RelationIndex index, final Locale locale){
		final Map<FLEFRecord, String> names = new IdentityHashMap<>(persons.size() * 2);
		for(final FLEFRecord person : persons)
			names.put(person, sortName(person, locale));

		final Comparator<FLEFRecord> byName = Comparator.comparing((final FLEFRecord person) -> names.get(person))
			.thenComparing(FLEFRecord::getId);
		final List<FLEFRecord> out = new ArrayList<>(persons);
		if(config.personOrder() == PersonOrder.BIRTH_DATE){
			final Map<FLEFRecord, Integer> years = new IdentityHashMap<>(persons.size() * 2);
			for(final FLEFRecord person : persons)
				years.put(person, birthYear(person, index));
			out.sort(Comparator.comparingInt((final FLEFRecord person) -> years.get(person)).thenComparing(byName));
		}
		else
			out.sort(byName);
		return out;
	}

	/** @return	The family name, then the other names, in lower case; whoever has no name sorts after everybody else. */
	private static String sortName(final FLEFRecord person, final Locale locale){
		final List<Name> names = NameAnatomyService.extractForIndividual(person);
		if(names.isEmpty())
			return "\uffff";

		final StringJoiner family = new StringJoiner(StringUtils.SPACE);
		final StringJoiner others = new StringJoiner(StringUtils.SPACE);
		for(final NamePart part : names.getFirst().parts()){
			final String value = part.value();
			if(value == null || value.isBlank())
				continue;
			if(NameReader.ENUM_PART_TYPE_FAMILY.equals(part.type()) || NameReader.ENUM_PART_TYPE_FAMILY_PREFIX.equals(part.type()))
				family.add(value.strip());
			else
				others.add(value.strip());
		}
		final String name = (family + StringUtils.SPACE + others).strip().toLowerCase(locale);
		return (name.isEmpty()? "\uffff": name);
	}

	/** @return	The year of birth, or the largest year if there is none (the person comes last). */
	private static int birthYear(final FLEFRecord person, final RelationIndex index){
		for(final FLEFRecord event : index.eventsOf(person))
			if(EventReader.ENUM_TYPE_BIRTH.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE))){
				final Integer year = GenealogicalDateHelper.yearOrNull(event);
				if(year != null)
					return year;
			}
		return Integer.MAX_VALUE;
	}

}
