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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * How well the file is backed by evidence: what has no citation, in how many cases, where the gaps concentrate and which lines of the
 * family are the least documented.
 * <ul>
 *   <li><b>By kind</b>: individuals, families, events, places and media, with the records that are not documented. An individual is
 *   documented when it, or one of its events, attributes or relationships, carries a supporting citation (the facts of a person are
 *   cited, rarely the person); a family is a partnership between two individuals, documented by a citation of the relationship or of the marriage event of the couple; a medium is documented when a source or an extract
 *   refers to it, as media do not carry citations of their own. A contradicting citation does not document what it contradicts;</li>
 *   <li><b>Hotspots</b>: the places and the kinds of event where most events lack a citation;</li>
 *   <li><b>Branches</b>: a branch is a founder (an individual with no known parent) and all their descendants; the branches are
 *   ordered from the least documented. Branches with the same members (a founder and the partner who married into the family)
 *   count once.</li>
 * </ul>
 * The analysis is read-only and respects the privacy policy: what is hidden is neither counted nor listed.
 *
 * @param byKind	One entry for each kind of record, in display order.
 * @param placeHotspots	The places with undocumented events, the worst first.
 * @param eventTypeHotspots	The kinds of event with undocumented events, the worst first.
 * @param branches	The branches with at least two individuals, the least documented first.
 */
public record DocumentationCoverage(List<KindCoverage> byKind, List<PlaceHotspot> placeHotspots, List<EventTypeHotspot> eventTypeHotspots,
		List<Branch> branches){

	/** What is measured. */
	public enum Kind{INDIVIDUAL, FAMILY, EVENT, PLACE, MEDIA}


	/**
	 * The coverage of one kind of record.
	 *
	 * @param kind	The kind.
	 * @param total	How many records of the kind are visible.
	 * @param undocumented	The records with no citation.
	 */
	public record KindCoverage(Kind kind, int total, List<FLEFRecord> undocumented){

		public int documented(){
			return total - undocumented.size();
		}

		/** @return	The share of documented records, in percent; 100 when there are none. */
		public double percentage(){
			return (total > 0? 100. * documented() / total: 100.);
		}

	}

	/**
	 * @param place	The place.
	 * @param undocumented	How many events held there carry no citation.
	 * @param total	How many events are held there.
	 */
	public record PlaceHotspot(FLEFRecord place, int undocumented, int total){}

	/**
	 * @param type	The type of the events, as written in the file ({@link #UNKNOWN_TYPE} when missing).
	 * @param undocumented	How many events of the type carry no citation.
	 * @param total	How many events of the type there are.
	 */
	public record EventTypeHotspot(String type, int undocumented, int total){}

	/**
	 * @param founder	The individual at the top of the branch.
	 * @param size	How many individuals the branch has, the founder included.
	 * @param documented	How many of them are documented.
	 */
	public record Branch(FLEFRecord founder, int size, int documented){

		public double percentage(){
			return (size > 0? 100. * documented / size: 100.);
		}

	}


	/** The type of the events that have none. */
	public static final String UNKNOWN_TYPE = "";
	/** The branches with fewer individuals than this are not reported: a lone founder is not a line. */
	private static final int MIN_BRANCH_SIZE = 2;


	/** @return	The coverage for the kind, or {@code null} if there is none. */
	public KindCoverage of(final Kind kind){
		for(final KindCoverage coverage : byKind)
			if(coverage.kind() == kind)
				return coverage;
		return null;
	}

	/** @return	The records with no citation, over every kind. */
	public int totalUndocumented(){
		int count = 0;
		for(final KindCoverage coverage : byKind)
			count += coverage.undocumented().size();
		return count;
	}

	/** @return	The share of documented records over every kind, in percent; 100 when there are none. */
	public double overallPercentage(){
		int total = 0;
		int documented = 0;
		for(final KindCoverage coverage : byKind){
			total += coverage.total();
			documented += coverage.documented();
		}
		return (total > 0? 100. * documented / total: 100.);
	}


	/**
	 * Reads the model.
	 *
	 * @param model	The model.
	 * @return	What is documented and what is not.
	 */
	public static DocumentationCoverage analyze(final FLEFModel model){
		final RelationIndex index = RelationIndex.build(model, record -> PrivacyService.isVisible(model, record));

		final List<FLEFRecord> individuals = PrivacyService.visible(model, model.getRecordsByType(IndividualReader.TYPE));
		final List<FLEFRecord> events = PrivacyService.visible(model, model.getRecordsByType(EventReader.TYPE));

		final Set<String> documentedIndividuals = documentedIndividuals(individuals, index);

		final List<KindCoverage> byKind = new ArrayList<>(Kind.values().length);
		byKind.add(new KindCoverage(Kind.INDIVIDUAL, individuals.size(), undocumentedIndividuals(individuals, documentedIndividuals)));
		byKind.add(families(model, index));
		byKind.add(new KindCoverage(Kind.EVENT, events.size(), undocumented(events)));
		final List<FLEFRecord> places = PrivacyService.visible(model, model.getRecordsByType(PlaceReader.TYPE));
		byKind.add(new KindCoverage(Kind.PLACE, places.size(), undocumented(places)));
		byKind.add(media(model));

		return new DocumentationCoverage(List.copyOf(byKind), placeHotspots(places, index), eventTypeHotspots(events),
			branches(individuals, documentedIndividuals, index));
	}

	private static boolean isDocumented(final FLEFRecord record){
		return SourceCitationReader.hasSupportingCitation(record);
	}

	private static List<FLEFRecord> undocumented(final List<FLEFRecord> records){
		final List<FLEFRecord> result = new ArrayList<>();
		for(final FLEFRecord record : records)
			if(!isDocumented(record))
				result.add(record);
		return result;
	}

	/** The ids of the individuals that have a supporting citation on themselves or on one of their facts. */
	private static Set<String> documentedIndividuals(final List<FLEFRecord> individuals, final RelationIndex index){
		final Set<String> documented = new HashSet<>();
		for(final FLEFRecord individual : individuals)
			if(isDocumented(individual) || anyDocumented(index.eventsOf(individual)) || anyDocumented(index.attributesOf(individual))
					|| anyDocumented(index.relationshipsOfSubject(individual)))
				documented.add(individual.getId());
		return documented;
	}

	private static boolean anyDocumented(final List<FLEFRecord> records){
		for(final FLEFRecord record : records)
			if(isDocumented(record))
				return true;
		return false;
	}

	private static List<FLEFRecord> undocumentedIndividuals(final List<FLEFRecord> individuals, final Set<String> documented){
		final List<FLEFRecord> result = new ArrayList<>();
		for(final FLEFRecord individual : individuals)
			if(!documented.contains(individual.getId()))
				result.add(individual);
		return result;
	}

	/**
	 * A family is a partnership: the relationship between two individuals that is a marriage, a union or a cohabitation. It is
	 * documented when the relationship carries a supporting citation, or when a marriage event of the two partners does.
	 */
	private static KindCoverage families(final FLEFModel model, final RelationIndex index){
		final List<FLEFRecord> partnerships = new ArrayList<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE)))
		{
			//the set of the types does not accept null
			final String type = RelationshipReader.extractType(relationship);
			if(type != null && RelationshipReader.isTypePartner(type))
				partnerships.add(relationship);
		}
		final List<FLEFRecord> undocumented = new ArrayList<>();
		for(final FLEFRecord partnership : partnerships)
			if(!isDocumented(partnership) && !hasDocumentedMarriage(partnership, model, index))
				undocumented.add(partnership);
		return new KindCoverage(Kind.FAMILY, partnerships.size(), undocumented);
	}

	/** Whether a marriage event that both partners take part in carries a supporting citation. */
	private static boolean hasDocumentedMarriage(final FLEFRecord partnership, final FLEFModel model, final RelationIndex index){
		final FLEFRecord subject = model.getRecordById(FLEFRecordHelper.getChildValue(partnership, RelationshipReader.TAG_SUBJECT_INDIVIDUAL));
		final FLEFRecord target = model.getRecordById(FLEFRecordHelper.getChildValue(partnership, RelationshipReader.TAG_TARGET_INDIVIDUAL));
		if(subject == null || target == null)
			return false;

		final Set<String> targetEvents = new HashSet<>();
		for(final FLEFRecord event : index.eventsOf(target))
			targetEvents.add(event.getId());
		for(final FLEFRecord event : index.eventsOf(subject))
			if(targetEvents.contains(event.getId()) && EventReader.ENUM_TYPE_MARRIAGE.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE))
					&& isDocumented(event))
				return true;
		return false;
	}

	/** A medium is documented when a source or an extract refers to it: the same rule as the unused documents of the citation analysis. */
	private static KindCoverage media(final FLEFModel model){
		final Map<String, Integer> usage = SourceHelper.countDocumentReferences(model);
		final List<FLEFRecord> all = PrivacyService.visible(model, model.getRecordsByType(MediaReader.TYPE));
		final List<FLEFRecord> unreferenced = new ArrayList<>();
		for(final FLEFRecord medium : all)
			if(usage.getOrDefault(medium.getId(), 0) == 0)
				unreferenced.add(medium);
		return new KindCoverage(Kind.MEDIA, all.size(), unreferenced);
	}

	private static List<PlaceHotspot> placeHotspots(final List<FLEFRecord> places, final RelationIndex index){
		final List<PlaceHotspot> hotspots = new ArrayList<>();
		for(final FLEFRecord place : places){
			final List<FLEFRecord> events = index.eventsAtPlace(place);
			final int undocumented = undocumented(events).size();
			if(undocumented > 0)
				hotspots.add(new PlaceHotspot(place, undocumented, events.size()));
		}
		hotspots.sort(Comparator.comparingInt(PlaceHotspot::undocumented).reversed()
			.thenComparing(Comparator.comparingInt(PlaceHotspot::total).reversed())
			.thenComparing(hotspot -> hotspot.place().getId()));
		return List.copyOf(hotspots);
	}

	private static List<EventTypeHotspot> eventTypeHotspots(final List<FLEFRecord> events){
		//the counts of each type: {undocumented, total}
		final Map<String, int[]> counts = new LinkedHashMap<>();
		for(final FLEFRecord event : events){
			final String type = FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE);
			final int[] count = counts.computeIfAbsent(type != null? type: UNKNOWN_TYPE, key -> new int[2]);
			count[1] ++;
			if(!isDocumented(event))
				count[0] ++;
		}

		final List<EventTypeHotspot> hotspots = new ArrayList<>();
		for(final Map.Entry<String, int[]> entry : counts.entrySet())
			if(entry.getValue()[0] > 0)
				hotspots.add(new EventTypeHotspot(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
		hotspots.sort(Comparator.comparingInt(EventTypeHotspot::undocumented).reversed()
			.thenComparing(EventTypeHotspot::type));
		return List.copyOf(hotspots);
	}

	private static List<Branch> branches(final List<FLEFRecord> individuals, final Set<String> documented, final RelationIndex index){
		final List<Branch> branches = new ArrayList<>();
		//branches with the same members are one branch (the partner who married in has the same descendants as the founder)
		final Set<Set<String>> seen = new HashSet<>();
		for(final FLEFRecord founder : individuals){
			if(!index.parentsOf(founder).isEmpty() || index.childrenOf(founder).isEmpty())
				continue;

			final Set<String> members = descendantsAndSelf(founder, index);
			if(members.size() < MIN_BRANCH_SIZE || !seen.add(members))
				continue;

			int documentedCount = 0;
			for(final String id : members)
				if(documented.contains(id))
					documentedCount ++;
			branches.add(new Branch(founder, members.size(), documentedCount));
		}
		branches.sort(Comparator.comparingDouble(Branch::percentage)
			.thenComparing(Comparator.comparingInt(Branch::size).reversed())
			.thenComparing(branch -> branch.founder().getId()));
		return List.copyOf(branches);
	}

	private static Set<String> descendantsAndSelf(final FLEFRecord founder, final RelationIndex index){
		final Map<String, FLEFRecord> reached = new HashMap<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();
		reached.put(founder.getId(), founder);
		queue.add(founder);
		while(!queue.isEmpty())
			for(final FLEFRecord child : index.childrenOf(queue.poll()))
				if(reached.putIfAbsent(child.getId(), child) == null)
					queue.add(child);
		return reached.keySet();
	}

}
