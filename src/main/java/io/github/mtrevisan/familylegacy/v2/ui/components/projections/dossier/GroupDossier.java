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
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Immutable snapshot of everything FLEF knows about a group.
 * <p>
 * Uses the same {@link DossierEntry} record as the individual dossier,
 * because the entry is generic enough to represent any FLEF assertion:
 * label, value, optional subtitle, optional evidence badge, and an
 * optional backing record for editing.
 */
public record GroupDossier(
	FLEFRecord group,
	String displayName,
	Map<GroupDossierSectionType, List<DossierEntry>> sections
){

	public GroupDossier{
		if(displayName == null)
			displayName = StringUtils.EMPTY;
		if(sections == null){
			final Map<GroupDossierSectionType, List<DossierEntry>> empty =
				new EnumMap<>(GroupDossierSectionType.class);
			for(final GroupDossierSectionType type : GroupDossierSectionType.values())
				empty.put(type, List.of());
			sections = empty;
		}
		else{
			final Map<GroupDossierSectionType, List<DossierEntry>> copy =
				new EnumMap<>(GroupDossierSectionType.class);
			for(final GroupDossierSectionType type : GroupDossierSectionType.values())
				copy.put(type, List.copyOf(sections.getOrDefault(type, List.of())));
			sections = copy;
		}
	}


	/**
	 * Returns an empty dossier, used when no group is selected or the
	 * group id does not resolve to a record.
	 *
	 * @return an empty dossier
	 */
	public static GroupDossier empty(){
		return new GroupDossier(null, StringUtils.EMPTY, null);
	}


	public List<DossierEntry> entriesOf(final GroupDossierSectionType type){
		return sections.getOrDefault(type, List.of());
	}

	public boolean isEmpty(){
		return (group == null);
	}

}
