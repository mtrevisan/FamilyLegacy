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
package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Finds the places where events of the tree happened, and whose jurisdiction or name changed over time: they were part of more than one
 * administrative area (one after the other), or they succeed, were merged from, split from or received territory from another place, or
 * they have a historic name besides the current one.
 * The finding only says that the history of the place is worth looking at (its timeline of jurisdictions already exists): it never
 * decides what the right hierarchy is.
 */
final class JurisdictionChangeChecker{

	private static final Set<String> SUCCESSIONS = Set.of(PlaceRelationshipReader.ENUM_TYPE_SUCCESSOR_OF,
		PlaceRelationshipReader.ENUM_TYPE_MERGED_FROM, PlaceRelationshipReader.ENUM_TYPE_SPLIT_FROM,
		PlaceRelationshipReader.ENUM_TYPE_RECEIVED_TERRITORY_FROM);


	private JurisdictionChangeChecker(){}


	/**
	 * @param data the places and their relationships
	 * @return one finding for each place that has events and changed jurisdiction, the busiest places first
	 */
	static List<Diagnostic> findings(final PlaceData data){
		final Map<String, Set<String>> administrativeParents = new HashMap<>();
		final Set<String> inSuccession = new HashSet<>();
		for(final PlaceData.Relation relation : data.relations()){
			if(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF.equals(relation.type()))
				administrativeParents.computeIfAbsent(relation.subjectId(), k -> new HashSet<>())
					.add(relation.targetId());
			else if(SUCCESSIONS.contains(relation.type())){
				inSuccession.add(relation.subjectId());
				inSuccession.add(relation.targetId());
			}
		}

		final List<Diagnostic> findings = new ArrayList<>();
		for(final PlaceData.Place place : data.places()){
			if(place.usage() <= 0)
				continue;

			final int parents = administrativeParents.getOrDefault(place.id(), Set.of()).size();
			final boolean succession = inSuccession.contains(place.id());
			if(parents < 2 && !succession && !place.historicName())
				continue;

			findings.add(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.PLACES,
				I18N.tf("place.jurisdiction_changed.title", place.label()),
				I18N.tf("place.jurisdiction_changed.explanation", place.label(), place.usage()),
				List.of(place.id()), new ShowJurisdictionTimelineAction(place.id()),
				DiagnosticKeys.of("place.jurisdiction_changed", List.of(place.id()), parents, succession, place.historicName())).withRank(place.usage()));
		}
		findings.sort((a, b) -> Double.compare(b.rank(), a.rank()));
		return findings;
	}

}
