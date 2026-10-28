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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The rules of the Context Assistant. They are pure: they read {@link ContextData} and say which historic events and cultural norms
 * may have influenced which events of the tree, with the reason.
 * <ul>
 *   <li>a historic event or a cultural norm of the model that was in effect, in the place, when an event of the tree happened, and that
 *   is of a kind that can influence an event of that type (a war and a death, a marriage law and a marriage): the draft of a context
 *   impact;</li>
 *   <li>deaths of several individuals in the same place in a short time, that no historic event of the model explains: the draft of a
 *   historic event (an epidemic, a famine...) to look for.</li>
 * </ul>
 * It is a suggestion that a context may matter, not a statement that it did: the researcher judges, and the impact is documented with
 * sources only if the researcher says so.
 */
final class ContextChecker{

	/** The most impacts proposed for the same historic event or cultural norm. */
	static final int MAX_PER_CONTEXT = 3;
	/** The most findings of a kind: the other ones are summed up in one finding. */
	static final int MAX_PER_KIND = 25;
	/** An event that may be anywhere in a longer time than this tells too little to be tied to a context (days). */
	static final long MAX_EVENT_SPAN = 5 * 366;
	/** The time in which the deaths of a place are taken to be a crisis (days). */
	static final long CLUSTER_WINDOW = 2 * 366;
	/** How many individuals dead in a place in the window are taken to be a crisis. */
	static final int CLUSTER_SIZE = 3;

	static final int HIGH_USEFULNESS = 70;
	static final int MEDIUM_USEFULNESS = 45;

	private static final Set<String> CRISIS_TYPES = Set.of("epidemic", "pandemic", "famine", "war", "natural_disaster");
	private static final Set<String> DEATH_EVENTS = Set.of("death", "burial");
	private static final Set<String> MARRIAGE_EVENTS = Set.of("marriage", "marriage_contract", "marriage_settlement", "marriage_bann",
		"marriage_license", "engagement");

	/**
	 * What a kind of historic event or cultural norm can influence.
	 *
	 * @param eventTypes the types of events of the tree it can influence
	 * @param impact     the type of impact to propose
	 * @param hint       the explanation of how it may matter (a key of the messages)
	 */
	private record Rule(Set<String> eventTypes, String impact, String hint){}

	private static final Map<String, Rule> HISTORIC = Map.ofEntries(
		Map.entry("war", new Rule(Set.of("death", "burial", "emigration", "immigration", "relocation", "military_service",
			"military_induction", "deportation", "internment"), "influences", "war")),
		Map.entry("epidemic", new Rule(DEATH_EVENTS, "explains", "epidemic")),
		Map.entry("pandemic", new Rule(DEATH_EVENTS, "explains", "epidemic")),
		Map.entry("famine", new Rule(DEATH_EVENTS, "explains", "famine")),
		Map.entry("natural_disaster", new Rule(Set.of("death", "burial", "relocation"), "explains", "disaster")),
		Map.entry("migration", new Rule(Set.of("emigration", "immigration", "relocation"), "explains", "migration")),
		Map.entry("legal_reform", new Rule(Set.of("marriage", "birth", "will", "probate", "guardianship", "adoption", "naturalization"),
			"influences", "law")),
		Map.entry("political_change", new Rule(Set.of("emigration", "immigration", "naturalization", "deportation"), "influences",
			"politics")),
		Map.entry("territorial_change", new Rule(Set.of("birth", "marriage", "relocation", "naturalization"), "influences",
			"territory")),
		Map.entry("economic_crisis", new Rule(Set.of("emigration", "relocation", "bankruptcy"), "motivates", "economy")),
		Map.entry("religious_reform", new Rule(Set.of("marriage", "birth", "burial"), "influences", "religion")));

	private static final Map<String, Rule> NORMS = Map.ofEntries(
		Map.entry("marriage_minimum_age", new Rule(MARRIAGE_EVENTS, "constrains", "marriage_age")),
		Map.entry("marriage_practice", new Rule(MARRIAGE_EVENTS, "explains", "marriage")),
		Map.entry("dowry_practice", new Rule(MARRIAGE_EVENTS, "explains", "marriage")),
		Map.entry("marriage_prohibited_degree", new Rule(MARRIAGE_EVENTS, "constrains", "marriage")),
		Map.entry("widowhood_rule", new Rule(MARRIAGE_EVENTS, "explains", "marriage")),
		Map.entry("baptism_age", new Rule(Set.of("birth"), "explains", "baptism")),
		Map.entry("confirmation_age", new Rule(Set.of("birth"), "explains", "baptism")),
		Map.entry("naming_convention", new Rule(Set.of("birth"), "explains", "naming")),
		Map.entry("surname_transmission", new Rule(Set.of("birth"), "explains", "naming")),
		Map.entry("patronymic_system", new Rule(Set.of("birth"), "explains", "naming")),
		Map.entry("matronymic_system", new Rule(Set.of("birth"), "explains", "naming")),
		Map.entry("legitimacy_rule", new Rule(Set.of("birth"), "explains", "legitimacy")),
		Map.entry("adoption_practice", new Rule(Set.of("adoption"), "explains", "adoption")),
		Map.entry("burial_practice", new Rule(DEATH_EVENTS, "explains", "burial")),
		Map.entry("inheritance_rule", new Rule(Set.of("death", "will", "probate", "guardianship"), "influences", "inheritance")),
		Map.entry("succession_rule", new Rule(Set.of("death", "will", "probate", "guardianship"), "influences", "inheritance")),
		Map.entry("guardianship_rule", new Rule(Set.of("death", "will", "probate", "guardianship"), "influences", "inheritance")),
		Map.entry("military_service_age", new Rule(Set.of("military_induction", "military_service"), "constrains", "military")));


	private ContextChecker(){}


	/**
	 * @param data the events, the historic events and cultural norms, and the impacts that exist
	 * @return the findings, in no particular order (each has its usefulness as rank)
	 */
	static List<ContextFinding> findings(final ContextData data){
		final List<ContextFinding> findings = new ArrayList<>();
		findings.addAll(capped(candidates(data)));
		findings.addAll(clusters(data));
		return findings;
	}

	/** @return how useful a suggestion with that estimated usefulness is: high, medium or low */
	static String level(final int usefulness){
		if(usefulness >= HIGH_USEFULNESS)
			return "high";
		return (usefulness >= MEDIUM_USEFULNESS? "medium": "low");
	}


	private static List<ContextFinding> candidates(final ContextData data){
		final Set<String> existing = new HashSet<>();
		for(final ContextData.Impact impact : data.impacts())
			existing.add(impact.contextId() + "|" + impact.targetId());

		final List<ContextFinding> findings = new ArrayList<>();
		for(final ContextData.Context context : data.contexts()){
			final Rule rule = (context.type() != null? (context.historic()? HISTORIC: NORMS).get(context.type()): null);
			if(rule == null || !hasBasis(context))
				continue;

			final List<ContextFinding> matches = new ArrayList<>();
			for(final ContextData.Event event : data.events()){
				if(!rule.eventTypes().contains(event.type()) || existing.contains(context.id() + "|" + event.id()))
					continue;

				final ContextFinding finding = candidate(context, rule, event, data);
				if(finding != null)
					matches.add(finding);
			}
			matches.sort(Comparator.comparingDouble((ContextFinding f) -> f.diagnostic().rank()).reversed());
			findings.addAll(matches.subList(0, Math.min(MAX_PER_CONTEXT, matches.size())));
		}
		return findings;
	}

	/** A historic event needs a date; a norm needs a date limit or a place: with neither it would be tied to everything. */
	private static boolean hasBasis(final ContextData.Context context){
		final DateRange range = context.range();
		if(context.historic())
			return (range != null && range.min() != Long.MIN_VALUE && range.max() != Long.MAX_VALUE);

		return ((range != null && (range.min() != Long.MIN_VALUE || range.max() != Long.MAX_VALUE)) || context.placeId() != null);
	}

	private static ContextFinding candidate(final ContextData.Context context, final Rule rule, final ContextData.Event event,
			final ContextData data){
		final DateRange range = event.range();
		if(range == null || range.min() == Long.MIN_VALUE || range.max() == Long.MAX_VALUE || range.max() - range.min() > MAX_EVENT_SPAN)
			return null;

		final DateRange period = context.range();
		if(period != null && !period.intersects(range))
			return null;
		//a context with no dates (a norm of a place) is in effect all the time
		final boolean inside = (period != null && period.min() <= range.min() && range.max() <= period.max());

		int usefulness = 35;
		final String where;
		if(context.placeId() != null){
			if(!isIn(event.placeId(), context.placeId(), data))
				return null;

			usefulness = 55;
			where = I18N.tf("context.why.place", data.placeLabels().getOrDefault(context.placeId(), context.placeId()));
		}
		else
			where = I18N.t("context.why.no_place");
		if(inside)
			usefulness += 10;

		final String when = (period != null? I18N.tf("context.why.period", period(period)): I18N.t("context.why.no_period"));
		final String why = when + "; " + where;
		final String hint = I18N.t("context.hint." + (context.historic()? "historic.": "norm.") + rule.hint());
		final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT,
			I18N.tf("context.candidate.title", context.title(), event.label()),
			I18N.tf("context.candidate.explanation", context.title(), event.label(), why, hint) + " "
				+ I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level(usefulness)), String.valueOf(usefulness)),
			List.of(context.id(), event.id()), null,
			DiagnosticKeys.of("context.candidate", List.of(context.id(), event.id()), rule.impact()))
			.withRank(usefulness);
		return new ContextFinding(diagnostic, new ContextFinding.ImpactDraft(context.id(), event.id(), rule.impact(),
			I18N.tf("context.impact.draft", context.title(), event.label(), why)), null);
	}

	/** @return whether a place is the other one or is contained in it, at any depth */
	private static boolean isIn(final String placeId, final String containerId, final ContextData data){
		return (placeId != null && (placeId.equals(containerId) || data.ancestors().getOrDefault(placeId, Set.of()).contains(containerId)));
	}

	private static String period(final DateRange range){
		return (range.min() == Long.MIN_VALUE? "...": String.valueOf(year(range.min())))
			+ "–" + (range.max() == Long.MAX_VALUE? "...": String.valueOf(year(range.max())));
	}

	private static int year(final long jdn){
		return CalendarType.jdnToProlepticGregorianLocalDate(jdn).getYear();
	}


	/** Deaths that follow one another in a place within a short time, and that no historic event of the model explains. */
	private static List<ContextFinding> clusters(final ContextData data){
		final Map<String, List<ContextData.Event>> byPlace = new java.util.HashMap<>();
		for(final ContextData.Event event : data.events())
			if(DEATH_EVENTS.contains(event.type()) && event.placeId() != null && event.range() != null
					&& event.range().min() != Long.MIN_VALUE && event.range().max() != Long.MAX_VALUE
					&& event.range().max() - event.range().min() <= 366)
				byPlace.computeIfAbsent(event.placeId(), k -> new ArrayList<>()).add(event);

		final List<ContextFinding> findings = new ArrayList<>();
		for(final Map.Entry<String, List<ContextData.Event>> entry : byPlace.entrySet()){
			final List<ContextData.Event> events = new ArrayList<>(entry.getValue());
			events.sort(Comparator.comparingLong((ContextData.Event e) -> e.range().min()));

			int start = 0;
			while(start < events.size()){
				int end = start;
				final Set<String> persons = new HashSet<>();
				while(end < events.size() && events.get(end).range().max() - events.get(start).range().min() <= CLUSTER_WINDOW){
					persons.addAll(events.get(end).principals());
					end ++;
				}
				if(persons.size() >= CLUSTER_SIZE){
					final List<ContextData.Event> cluster = events.subList(start, end);
					final ContextFinding finding = cluster(entry.getKey(), cluster, persons.size(), data);
					if(finding != null)
						findings.add(finding);
					start = end;
				}
				else
					start ++;
			}
		}
		return capped(findings);
	}

	private static ContextFinding cluster(final String placeId, final List<ContextData.Event> cluster, final int persons,
			final ContextData data){
		final DateRange span = cluster.getFirst().range().hull(cluster.getLast().range());
		for(final ContextData.Context context : data.contexts())
			if(context.historic() && CRISIS_TYPES.contains(context.type()) && context.range() != null
					&& context.range().intersects(span) && (context.placeId() == null || isIn(placeId, context.placeId(), data)))
				return null;

		final String place = data.placeLabels().getOrDefault(placeId, placeId);
		final String years = (year(span.min()) == year(span.max())? String.valueOf(year(span.min()))
			: year(span.min()) + "–" + year(span.max()));
		final int usefulness = Math.min(70, 40 + 5 * persons);
		final List<String> ids = new ArrayList<>();
		ids.add(placeId);
		cluster.forEach(event -> ids.add(event.id()));
		final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT,
			I18N.tf("context.cluster.title", place, years),
			I18N.tf("context.cluster.explanation", String.valueOf(persons), place, years) + " "
				+ I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level(usefulness)), String.valueOf(usefulness)),
			ids, null, DiagnosticKeys.of("context.cluster", List.of(placeId), years, persons)).withRank(usefulness);
		return new ContextFinding(diagnostic, null, new ContextFinding.HistoricDraft("epidemic", I18N.tf("context.cluster.draft.title",
			place, years)));
	}


	/** Keeps the findings with the highest rank, up to {@link #MAX_PER_KIND}, and sums up the other ones in a single finding. */
	private static List<ContextFinding> capped(final List<ContextFinding> findings){
		if(findings.size() <= MAX_PER_KIND)
			return findings;

		final List<ContextFinding> sorted = new ArrayList<>(findings);
		sorted.sort(Comparator.comparingDouble((ContextFinding f) -> f.diagnostic().rank()).reversed());
		final List<ContextFinding> result = new ArrayList<>(sorted.subList(0, MAX_PER_KIND));
		final int more = sorted.size() - MAX_PER_KIND;
		final String kind = (sorted.getFirst().impact() != null? "context.candidate": "context.cluster");
		result.add(new ContextFinding(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT,
			I18N.tf(kind + ".more.title", String.valueOf(more)), I18N.tf(kind + ".more.explanation", String.valueOf(more),
			String.valueOf(MAX_PER_KIND)), List.of(), null, DiagnosticKeys.of(kind + ".more", List.of(), more))
			.withRank(sorted.get(MAX_PER_KIND).diagnostic().rank()), null, null));
		return result;
	}

}
