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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Immutable snapshot of everything FLEF knows about a single individual.
 * <p>
 * The dossier is produced by {@link IndividualDossierService} and consumed
 * by {@link IndividualDossierPanel}. It contains:
 * <ul>
 *   <li>the backing {@code FLEFRecord} of the individual, for editing;</li>
 *   <li>the {@link IndividualData} used to display name, sex, image and
 *       vital dates;</li>
 *   <li>the display name, pre-computed once so the panel does not need to
 *       invoke the individual handler on every repaint;</li>
 *   <li>a map from {@link DossierSectionType} to the list of
 *       {@link DossierEntry} rows that belong to that section.</li>
 * </ul>
 * Empty sections are still present in the map with an empty list, so the
 * panel can decide whether to show or hide them consistently.
 */
public record IndividualDossier(
	FLEFRecord individual,
	IndividualData data,
	String displayName,
	Map<DossierSectionType, List<DossierEntry>> sections
){

	public IndividualDossier{
		if(displayName == null)
			displayName = StringUtils.EMPTY;
		if(sections == null){
			final Map<DossierSectionType, List<DossierEntry>> empty = new EnumMap<>(DossierSectionType.class);
			for(final DossierSectionType type : DossierSectionType.values())
				empty.put(type, List.of());
			sections = empty;
		}
		else{
			final Map<DossierSectionType, List<DossierEntry>> copy = new EnumMap<>(DossierSectionType.class);
			for(final DossierSectionType type : DossierSectionType.values())
				copy.put(type, List.copyOf(sections.getOrDefault(type, List.of())));
			sections = copy;
		}
	}


	/**
	 * Returns an empty dossier, used when no individual is selected or
	 * the individual id does not resolve to a record.
	 *
	 * @return an empty dossier
	 */
	public static IndividualDossier empty(){
		return new IndividualDossier(null, null, StringUtils.EMPTY, null);
	}


	/**
	 * Returns the entries of the given section. Never {@code null}.
	 *
	 * @param type the section type
	 * @return the list of entries, possibly empty
	 */
	public List<DossierEntry> entriesOf(final DossierSectionType type){
		return sections.getOrDefault(type, List.of());
	}

	/**
	 * Returns whether the dossier has no individual.
	 *
	 * @return {@code true} if the backing individual is {@code null}
	 */
	public boolean isEmpty(){
		return (individual == null);
	}

	/**
	 * Returns the total number of entries across all sections.
	 *
	 * @return the total entry count
	 */
	public int totalEntryCount(){
		int count = 0;
		for(final List<DossierEntry> list : sections.values())
			count += list.size();
		return count;
	}

}
