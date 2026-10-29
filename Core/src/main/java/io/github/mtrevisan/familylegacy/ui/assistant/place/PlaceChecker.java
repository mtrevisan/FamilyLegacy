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

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The rules of the Place Assistant. They are pure: they read {@link PlaceData} and say what looks wrong or missing, with the reason.
 * <p>
 * Nothing is decided here: a possible duplicate is never merged, a missing relationship is only proposed as a draft.
 * </p>
 * <ul>
 *   <li>places that may be the same place (same name and compatible type, not related, not clearly in different places);</li>
 *   <li>a place that is part of nothing, though its name says where it is (<em>Treviso, Veneto</em>) or it is used by events;</li>
 *   <li>a parish that belongs to no diocese;</li>
 *   <li>a place that is part of a smaller one (a province in a city), or of itself;</li>
 *   <li>places that are part of each other, or that succeed each other, in a circle.</li>
 * </ul>
 */
final class PlaceChecker{

	/** The most findings reported for the same kind of problem: the other ones are summed up in one finding. */
	static final int MAX_PER_KIND = 25;

	/** Places closer than this are probably the same place (km). */
	static final double NEAR_KM = 5.;
	/** Places farther than this are not the same place, whatever their names say (km). */
	static final double FAR_KM = 50.;

	private static final Set<String> PART_OF = Set.of("administrative_part_of", "geographic_part_of", "ecclesiastical_part_of",
		"judicial_part_of", "cadastral_part_of");
	private static final Set<String> TERRITORIAL = Set.of("administrative_part_of", "geographic_part_of");
	private static final Set<String> SUCCESSION = Set.of("successor_of", "merged_from", "split_from", "received_territory_from");
	private static final String ECCLESIASTICAL = "ecclesiastical_part_of";

	/** How large the types of place are (only the ones that can be compared: a parish or a district is not on the scale). */
	private static final Map<String, Integer> SCALE = Map.ofEntries(
		Map.entry("address", 0), Map.entry("building", 1), Map.entry("street", 1), Map.entry("cemetery", 1), Map.entry("hamlet", 2),
		Map.entry("village", 3), Map.entry("town", 4), Map.entry("municipality", 4), Map.entry("city", 4),
		Map.entry("metropolitan_area", 5), Map.entry("county", 6), Map.entry("province", 6), Map.entry("department", 6),
		Map.entry("region", 7), Map.entry("macro_region", 8), Map.entry("country", 9), Map.entry("empire", 10));
	private static final int COUNTRY_SCALE = SCALE.get("country");

	private static final Pattern COORDINATES = Pattern.compile("^([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");


	private PlaceChecker(){}


	/**
	 * Parses coordinates written in ISO 6709 (e.g. {@code +45.65-012.2/}).
	 *
	 * @param text the coordinates (can be {@code null})
	 * @return the latitude and the longitude in degrees, or {@code null} if the text is not a pair of coordinates
	 */
	static double[] parseCoordinates(final String text){
		if(text == null)
			return null;

		final Matcher matcher = COORDINATES.matcher(text.strip());
		if(!matcher.find())
			return null;

		final double lat = Double.parseDouble(matcher.group(1));
		final double lon = Double.parseDouble(matcher.group(2));
		return (Math.abs(lat) <= 90. && Math.abs(lon) <= 180.? new double[]{lat, lon}: null);
	}

	private static Integer scale(final String type){
		return (type != null? SCALE.get(type): null);
	}

	/** @return the name without case, accents and punctuation, to compare names that are written in different ways */
	static String normalize(final String name){
		final String decomposed = Normalizer.normalize(name, Normalizer.Form.NFD);
		return NOT_ALPHANUMERIC.matcher(DIACRITICS.matcher(decomposed).replaceAll(""))
			.replaceAll(StringUtils.SPACE)
			.strip()
			.toLowerCase(Locale.ROOT);
	}

	/** @return the distance between two places on the surface of the Earth (km) */
	static double distanceKm(final PlaceData.Place a, final PlaceData.Place b){
		final double lat1 = Math.toRadians(a.lat());
		final double lat2 = Math.toRadians(b.lat());
		final double dLat = lat2 - lat1;
		final double dLon = Math.toRadians(b.lon() - a.lon());
		final double h = Math.sin(dLat / 2.) * Math.sin(dLat / 2.)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2.) * Math.sin(dLon / 2.);
		return 2. * 6371. * Math.asin(Math.min(1., Math.sqrt(h)));
	}


	/**
	 * @param data the places and their relationships
	 * @return the findings, in no particular order
	 */
	static List<PlaceFinding> findings(final PlaceData data){
		final Map<String, PlaceData.Place> byId = new LinkedHashMap<>(data.places().size());
		for(final PlaceData.Place place : data.places())
			byId.put(place.id(), place);

		final List<PlaceFinding> findings = new ArrayList<>();
		findings.addAll(capped("duplicate", duplicates(data, byId)));
		findings.addAll(brokenRelations(data, byId));
		findings.addAll(cycles(data, byId));
		findings.addAll(capped("no_parent", missingParents(data, byId)));
		findings.addAll(capped("no_diocese", missingDioceses(data, byId)));
		return findings;
	}


	private static List<PlaceFinding> duplicates(final PlaceData data, final Map<String, PlaceData.Place> byId){
		final Map<String, List<PlaceData.Place>> byName = new LinkedHashMap<>();
		for(final PlaceData.Place place : data.places()){
			final Set<String> keys = new TreeSet<>();
			for(final String name : place.names()){
				final String key = normalize(name);
				if(!key.isEmpty())
					keys.add(key);
			}
			for(final String key : keys)
				byName.computeIfAbsent(key, k -> new ArrayList<>()).add(place);
		}

		final Set<String> related = new HashSet<>();
		for(final PlaceData.Relation relation : data.relations()){
			related.add(relation.subjectId() + "|" + relation.targetId());
			related.add(relation.targetId() + "|" + relation.subjectId());
		}
		final Map<String, Set<String>> parents = parents(data);

		final List<PlaceFinding> findings = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for(final Map.Entry<String, List<PlaceData.Place>> entry : byName.entrySet()){
			final List<PlaceData.Place> group = entry.getValue();
			for(int i = 0; i < group.size() - 1; i ++)
				for(int j = i + 1; j < group.size(); j ++){
					final PlaceData.Place left = group.get(i);
					final PlaceData.Place right = group.get(j);
					final String pair = (left.id().compareTo(right.id()) < 0? left.id() + "|" + right.id(): right.id() + "|" + left.id());
					if(related.contains(left.id() + "|" + right.id()) || !seen.add(pair))
						continue;

					final PlaceFinding finding = duplicate(left, right, entry.getKey(), parents, byId);
					if(finding != null)
						findings.add(finding);
				}
		}
		return findings;
	}

	private static PlaceFinding duplicate(final PlaceData.Place left, final PlaceData.Place right, final String name,
			final Map<String, Set<String>> parents, final Map<String, PlaceData.Place> byId){
		final boolean sameType = (left.type() != null && left.type().equals(right.type()));
		//a city and a province may share the name: they are different places
		if(left.type() != null && right.type() != null && !sameType)
			return null;

		final List<String> why = new ArrayList<>();
		why.add(I18N.t("place.why.same_name"));
		if(sameType)
			why.add(I18N.tf("place.why.same_type", I18N.t("enum.place.type." + left.type(), left.type())));

		//a place with the same name in another place is another place (San Pietro in two different towns)
		final Set<String> leftParents = parents.getOrDefault(left.id(), Set.of());
		final Set<String> rightParents = parents.getOrDefault(right.id(), Set.of());
		if(!leftParents.isEmpty() && !rightParents.isEmpty()){
			if(leftParents.stream().noneMatch(rightParents::contains) && !sameParentName(leftParents, rightParents, byId))
				return null;

			why.add(I18N.t("place.why.same_parent"));
		}

		if(left.hasCoordinates() && right.hasCoordinates()){
			final double distance = distanceKm(left, right);
			if(distance > FAR_KM)
				return null;

			why.add(I18N.tf("place.why.near", String.valueOf(Math.round(distance * 10.) / 10.)));
		}
		else
			why.add(I18N.t("place.why.no_coordinates"));

		final Diagnostic diagnostic = new Diagnostic((sameType? Diagnostic.Severity.WARNING: Diagnostic.Severity.INFORMATION),
			AssistantCategory.PLACES, I18N.tf("place.possible_duplicate.title", label(left), label(right)),
			I18N.tf("place.possible_duplicate.explanation", label(left), label(right), String.join("; ", why)),
			List.of(left.id(), right.id()), null,
			DiagnosticKeys.of("place.possible_duplicate", List.of(left.id(), right.id()), name, left.type(), right.type()))
			.withRank(left.usage() + right.usage());
		return new PlaceFinding(diagnostic, null, List.of(left.id(), right.id()));
	}

	private static boolean sameParentName(final Set<String> leftParents, final Set<String> rightParents,
			final Map<String, PlaceData.Place> byId){
		final Set<String> names = new HashSet<>();
		for(final String id : leftParents){
			final PlaceData.Place place = byId.get(id);
			if(place != null)
				place.names().forEach(name -> names.add(normalize(name)));
		}
		for(final String id : rightParents){
			final PlaceData.Place place = byId.get(id);
			if(place != null && place.names().stream().map(PlaceChecker::normalize).anyMatch(names::contains))
				return true;
		}
		return false;
	}

	/** The places that contain a place, by the relationships of the territory (the ones of succession do not say where a place is). */
	private static Map<String, Set<String>> parents(final PlaceData data){
		final Map<String, Set<String>> parents = new HashMap<>();
		for(final PlaceData.Relation relation : data.relations())
			if(PART_OF.contains(relation.type()))
				parents.computeIfAbsent(relation.subjectId(), k -> new HashSet<>()).add(relation.targetId());
		return parents;
	}


	private static List<PlaceFinding> brokenRelations(final PlaceData data, final Map<String, PlaceData.Place> byId){
		final List<PlaceFinding> findings = new ArrayList<>();
		for(final PlaceData.Relation relation : data.relations()){
			final PlaceData.Place subject = byId.get(relation.subjectId());
			final PlaceData.Place target = byId.get(relation.targetId());
			if(subject == null || target == null)
				continue;

			if(subject == target){
				findings.add(finding(Diagnostic.Severity.ERROR, "place.self_relation", List.of(relation.id(), subject.id()),
					I18N.tf("place.self_relation.title", label(subject)), I18N.tf("place.self_relation.explanation", label(subject)),
					0., null, relation.type()));
				continue;
			}

			if(!TERRITORIAL.contains(relation.type()))
				continue;

			final Integer subjectScale = scale(subject.type());
			final Integer targetScale = scale(target.type());
			if(subjectScale != null && targetScale != null && subjectScale > targetScale)
				findings.add(finding(Diagnostic.Severity.WARNING, "place.inverted_hierarchy",
					List.of(relation.id(), subject.id(), target.id()),
					I18N.tf("place.inverted_hierarchy.title", label(subject), label(target)),
					I18N.tf("place.inverted_hierarchy.explanation", label(subject), label(target),
						I18N.t("enum.place.type." + subject.type(), subject.type()),
						I18N.t("enum.place.type." + target.type(), target.type())),
					0., null, relation.type()));
		}
		return findings;
	}


	private static List<PlaceFinding> cycles(final PlaceData data, final Map<String, PlaceData.Place> byId){
		final List<PlaceFinding> findings = new ArrayList<>();
		findings.addAll(cycles(data, byId, PART_OF, Diagnostic.Severity.ERROR, "place.cycle.part_of"));
		findings.addAll(cycles(data, byId, SUCCESSION, Diagnostic.Severity.WARNING, "place.cycle.succession"));
		return findings;
	}

	private static List<PlaceFinding> cycles(final PlaceData data, final Map<String, PlaceData.Place> byId, final Set<String> types,
			final Diagnostic.Severity severity, final String kind){
		final Map<String, List<String>> graph = new LinkedHashMap<>();
		for(final PlaceData.Relation relation : data.relations())
			if(types.contains(relation.type()) && byId.containsKey(relation.subjectId()) && byId.containsKey(relation.targetId())
					&& !relation.subjectId().equals(relation.targetId()))
				graph.computeIfAbsent(relation.subjectId(), k -> new ArrayList<>()).add(relation.targetId());

		final List<PlaceFinding> findings = new ArrayList<>();
		for(final Set<String> component : new Tarjan(graph).components()){
			final Set<String> ids = new TreeSet<>(component);
			data.relations().stream()
				.filter(r -> types.contains(r.type()) && component.contains(r.subjectId()) && component.contains(r.targetId()))
				.forEach(r -> ids.add(r.id()));
			final String names = String.join(", ", component.stream().map(byId::get).map(PlaceData.Place::label).sorted().toList());
			findings.add(finding(severity, kind, new ArrayList<>(ids), I18N.tf(kind + ".title", names),
				I18N.tf(kind + ".explanation", names), 0., null, String.valueOf(component.size())));
		}
		return findings;
	}

	/** The groups of places that can all be reached from one another (only the ones of more than one place). */
	private static final class Tarjan{

		private final Map<String, List<String>> graph;
		private final Map<String, Integer> index = new HashMap<>();
		private final Map<String, Integer> low = new HashMap<>();
		private final List<String> stack = new ArrayList<>();
		private final Set<String> onStack = new HashSet<>();
		private final List<Set<String>> components = new ArrayList<>();


		Tarjan(final Map<String, List<String>> graph){
			this.graph = graph;
		}


		List<Set<String>> components(){
			for(final String node : new ArrayList<>(graph.keySet()))
				if(!index.containsKey(node))
					visit(node);
			return components;
		}

		private void visit(final String node){
			index.put(node, index.size());
			low.put(node, index.get(node));
			stack.add(node);
			onStack.add(node);

			for(final String next : graph.getOrDefault(node, List.of())){
				if(!index.containsKey(next)){
					visit(next);
					low.put(node, Math.min(low.get(node), low.get(next)));
				}
				else if(onStack.contains(next))
					low.put(node, Math.min(low.get(node), index.get(next)));
			}

			if(low.get(node).equals(index.get(node))){
				final Set<String> component = new TreeSet<>();
				String member;
				do{
					member = stack.remove(stack.size() - 1);
					onStack.remove(member);
					component.add(member);
				}while(!member.equals(node));
				if(component.size() > 1)
					components.add(component);
			}
		}

	}


	private static List<PlaceFinding> missingParents(final PlaceData data, final Map<String, PlaceData.Place> byId){
		final Map<String, Set<String>> parents = parents(data);
		final Map<String, List<PlaceData.Place>> byName = indexByName(data);

		final List<PlaceFinding> findings = new ArrayList<>();
		for(final PlaceData.Place place : data.places()){
			final Integer scale = scale(place.type());
			if(scale == null || scale >= COUNTRY_SCALE || parents.containsKey(place.id()))
				continue;

			final PlaceData.Place container = container(place, scale, byName);
			if(container == null && place.usage() == 0)
				continue;

			final PlaceDraft draft = new PlaceDraft(place.id(), (container != null? container.id(): null),
				"administrative_part_of");
			final List<String> ids = (container != null? List.of(place.id(), container.id()): List.of(place.id()));
			findings.add(finding(Diagnostic.Severity.INFORMATION, "place.no_parent", ids,
				I18N.tf("place.no_parent.title", label(place)),
				(container != null
					? I18N.tf("place.no_parent.explanation.named", label(place), label(container))
					: I18N.tf("place.no_parent.explanation.used", label(place), String.valueOf(place.usage()))),
				place.usage() + (container != null? 1: 0), draft, null));
		}
		return findings;
	}

	/**
	 * The place that the name of a place says contains it: the name <em>Treviso, Veneto</em> names the place Veneto, if it is one place
	 * and larger than Treviso.
	 */
	private static PlaceData.Place container(final PlaceData.Place place, final int scale,
			final Map<String, List<PlaceData.Place>> byName){
		for(final String name : place.names()){
			final String[] parts = StringUtils.splitPreserveAllTokens(name, ',');
			for(int i = 1; i < parts.length; i ++){
				final List<PlaceData.Place> candidates = byName.getOrDefault(normalize(parts[i]), List.of()).stream()
					.filter(candidate -> !candidate.id().equals(place.id()))
					.filter(candidate -> {
						final Integer candidateScale = scale(candidate.type());
						return (candidateScale != null && candidateScale > scale);
					})
					.toList();
				if(candidates.size() == 1)
					return candidates.getFirst();
			}
		}
		return null;
	}

	private static Map<String, List<PlaceData.Place>> indexByName(final PlaceData data){
		final Map<String, List<PlaceData.Place>> byName = new HashMap<>();
		for(final PlaceData.Place place : data.places())
			for(final String name : place.names()){
				final String key = normalize(name);
				if(!key.isEmpty())
					byName.computeIfAbsent(key, k -> new ArrayList<>()).add(place);
			}
		return byName;
	}


	private static List<PlaceFinding> missingDioceses(final PlaceData data, final Map<String, PlaceData.Place> byId){
		final List<PlaceData.Place> dioceses = data.places().stream().filter(place -> "diocese".equals(place.type())).toList();
		final List<PlaceData.Place> archdioceses = data.places().stream().filter(place -> "archdiocese".equals(place.type())).toList();
		final PlaceData.Place proposed = (dioceses.size() == 1? dioceses.getFirst()
			: (dioceses.isEmpty() && archdioceses.size() == 1? archdioceses.getFirst(): null));

		final Map<String, Set<String>> parents = parents(data);
		final Set<String> subjectsOfEcclesiastical = new HashSet<>();
		for(final PlaceData.Relation relation : data.relations()){
			final PlaceData.Place target = byId.get(relation.targetId());
			if(ECCLESIASTICAL.equals(relation.type()) || (PART_OF.contains(relation.type()) && target != null
					&& ("diocese".equals(target.type()) || "archdiocese".equals(target.type()))))
				subjectsOfEcclesiastical.add(relation.subjectId());
		}

		final List<PlaceFinding> findings = new ArrayList<>();
		for(final PlaceData.Place place : data.places()){
			if(!"parish".equals(place.type()) || subjectsOfEcclesiastical.contains(place.id()))
				continue;

			final PlaceDraft draft = new PlaceDraft(place.id(), (proposed != null? proposed.id(): null), ECCLESIASTICAL);
			final List<String> ids = (proposed != null? List.of(place.id(), proposed.id()): List.of(place.id()));
			findings.add(finding(Diagnostic.Severity.INFORMATION, "place.no_diocese", ids,
				I18N.tf("place.no_diocese.title", label(place)),
				(proposed != null
					? I18N.tf("place.no_diocese.explanation.one", label(place), label(proposed))
					: I18N.tf("place.no_diocese.explanation", label(place))),
				place.usage(), draft, null));
		}
		return findings;
	}


	/**
	 * Keeps the findings with the highest rank, up to {@link #MAX_PER_KIND}, and sums up the other ones in a single finding.
	 */
	private static List<PlaceFinding> capped(final String kind, final List<PlaceFinding> findings){
		if(findings.size() <= MAX_PER_KIND)
			return findings;

		final List<PlaceFinding> sorted = new ArrayList<>(findings);
		sorted.sort(Comparator.comparingDouble((PlaceFinding f) -> f.diagnostic().rank()).reversed());
		final List<PlaceFinding> result = new ArrayList<>(sorted.subList(0, MAX_PER_KIND));
		final int more = sorted.size() - MAX_PER_KIND;
		result.add(new PlaceFinding(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.PLACES,
			I18N.tf("place." + kind + ".more.title", String.valueOf(more)),
			I18N.tf("place." + kind + ".more.explanation", String.valueOf(more), String.valueOf(MAX_PER_KIND)), List.of(), null,
			DiagnosticKeys.of("place." + kind + ".more", List.of(), more)), null, List.of()));
		return result;
	}

	private static PlaceFinding finding(final Diagnostic.Severity severity, final String kind, final List<String> ids,
			final String title, final String explanation, final double rank, final PlaceDraft draft, final Object data){
		final Diagnostic diagnostic = new Diagnostic(severity, AssistantCategory.PLACES, title, explanation, ids, null,
			DiagnosticKeys.of(kind, ids, data)).withRank(rank);
		return new PlaceFinding(diagnostic, draft, List.of());
	}

	private static String label(final PlaceData.Place place){
		return (place.type() != null
			? place.label() + " (" + I18N.t("enum.place.type." + place.type(), place.type()) + ")"
			: place.label());
	}

}
