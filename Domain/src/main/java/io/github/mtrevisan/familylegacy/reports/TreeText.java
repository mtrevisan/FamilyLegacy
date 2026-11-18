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
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


/** What the trees of a report (the ancestors, the descendants) write about a person, and the order they put the children in. */
final class TreeText{

	private final ReportContext ctx;
	private final Map<FLEFRecord, NormalizedDate> births = new IdentityHashMap<>();


	TreeText(final ReportContext ctx){
		this.ctx = ctx;
	}


	/** @return	The name of the person with the years of birth and death, as in {@code Antonia Pastrelo [I1430] (1633–?)} (not escaped). */
	String label(final FLEFRecord person){
		final String name = ctx.displayText(person);
		final String years = years(person);
		return (years != null? name + " (" + years + ")": name);
	}

	/** @return	The years of birth and death of the person ({@code 1633–?}, {@code ?–1654}), {@code null} if none is known. */
	String years(final FLEFRecord person){
		final Integer birth = yearOf(person, EventReader.ENUM_TYPE_BIRTH);
		final Integer death = yearOf(person, EventReader.ENUM_TYPE_DEATH);
		if(birth == null && death == null)
			return null;

		return (birth != null? birth.toString(): "?") + "–" + (death != null? death.toString(): "?");
	}

	/** @return	The children, the eldest first; those with no date of birth come last, in the order of the file. */
	List<FLEFRecord> childrenOf(final FLEFRecord parent){
		final List<FLEFRecord> children = new ArrayList<>(ctx.index.childrenOf(parent));
		children.sort(Comparator.comparing(this::birthOf, Comparator.nullsLast(Comparator.naturalOrder())));
		return children;
	}


	/** @return	An order of persons by date of birth, the eldest first; those with no date of birth come last. */
	Comparator<FLEFRecord> byBirth(){
		return Comparator.comparing(this::birthOf, Comparator.nullsLast(Comparator.naturalOrder()));
	}

	private NormalizedDate birthOf(final FLEFRecord person){
		return births.computeIfAbsent(person, p -> {
			for(final FLEFRecord event : ctx.index.eventsOf(p))
				if(EventReader.ENUM_TYPE_BIRTH.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE))){
					final TemporalSpan span = DateReader.extractTemporalSpan(FLEFRecordHelper.findChild(event, EventReader.TAG_DATE));
					if(span != null && span.start() != null)
						return span.start();
				}
			return null;
		});
	}

	private Integer yearOf(final FLEFRecord person, final String type){
		for(final FLEFRecord event : ctx.index.eventsOf(person))
			if(type.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE))){
				final Integer year = GenealogicalDateHelper.yearOrNull(event);
				if(year != null)
					return year;
			}
		return null;
	}

}
