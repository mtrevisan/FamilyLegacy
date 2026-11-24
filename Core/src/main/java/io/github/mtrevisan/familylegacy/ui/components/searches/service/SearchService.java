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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.places.PlaceNameResolver;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.search.AttachedTextExtractor;
import io.github.mtrevisan.familylegacy.search.Bm25;
import io.github.mtrevisan.familylegacy.search.GroupMemberNames;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchField;
import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.search.SearchQuery;
import io.github.mtrevisan.familylegacy.search.SearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.ConclusionSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.CulturalNormSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.EventParticipationSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.EventSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.GroupSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.HistoricEventSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.IdentityHypothesisSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.IndividualSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.MediaSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.PlaceRelationshipSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.PlaceSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.RelationshipSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.RepositorySearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.ResearchActivitySearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.ResearchQuestionSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.ResearchTaskSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.SourceSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Service that executes an advanced search using a strategy registry and
 * a word-level text matcher.
 * <p>
 * The search has two stages:
 * <ol>
 *   <li>the <b>structural filters</b> (sex, event type, date range, place,
 *       ...) are applied by the per-type {@link SearchStrategy};</li>
 *   <li>the <b>text query</b> is scored against the display text of each
 *       surviving record by {@link SearchMatcher}.</li>
 * </ol>
 * The results are ranked by the number of matched query tokens first, by
 * the sum of the per-token fuzzy scores second, and by display text third
 * (stable, alphabetical tie-breaker). A record that matches both words of
 * a two-word query always ranks above a record that matches only one.
 * <p>
 * When the query is blank, the text stage is skipped and the results are
 * the structurally-filtered records in their original order.
 */
public class SearchService{

	private static final Map<Class<? extends RecordTypeHandler<?>>, SearchStrategy> REGISTRY = new HashMap<>();
	static{
		REGISTRY.put(ConclusionHandler.class, new ConclusionSearchStrategy());
		REGISTRY.put(CulturalNormHandler.class, new CulturalNormSearchStrategy());
		REGISTRY.put(MediaHandler.class, new MediaSearchStrategy());
		REGISTRY.put(EventHandler.class, new EventSearchStrategy());
		REGISTRY.put(EventParticipationHandler.class, new EventParticipationSearchStrategy());
		REGISTRY.put(GroupHandler.class, new GroupSearchStrategy());
		REGISTRY.put(HistoricEventHandler.class, new HistoricEventSearchStrategy());
		REGISTRY.put(IdentityHypothesisHandler.class, new IdentityHypothesisSearchStrategy());
		REGISTRY.put(IndividualHandler.class, new IndividualSearchStrategy());
		REGISTRY.put(PlaceHandler.class, new PlaceSearchStrategy());
		REGISTRY.put(RelationshipHandler.class, new RelationshipSearchStrategy());
		REGISTRY.put(PlaceRelationshipHandler.class, new PlaceRelationshipSearchStrategy());
		REGISTRY.put(RepositoryHandler.class, new RepositorySearchStrategy());
		REGISTRY.put(ResearchActivityHandler.class, new ResearchActivitySearchStrategy());
		REGISTRY.put(ResearchQuestionHandler.class, new ResearchQuestionSearchStrategy());
		REGISTRY.put(ResearchTaskHandler.class, new ResearchTaskSearchStrategy());
		REGISTRY.put(SourceHandler.class, new SourceSearchStrategy());
	}


	/**
	 * @return	The types of record that can be searched, in the order to offer them: the people and their groups first, then the
	 * 	events, places and sources, the research, the context. Every type with a strategy is there, and only those.
	 */
	public static List<Class<? extends RecordTypeHandler<?>>> searchableHandlers(){
		return List.of(
			IndividualHandler.class, GroupHandler.class, EventHandler.class, EventParticipationHandler.class, PlaceHandler.class, SourceHandler.class,
			RepositoryHandler.class, MediaHandler.class, RelationshipHandler.class, PlaceRelationshipHandler.class,
			ConclusionHandler.class, IdentityHypothesisHandler.class, ResearchQuestionHandler.class, ResearchActivityHandler.class,
			ResearchTaskHandler.class, HistoricEventHandler.class, CulturalNormHandler.class);
	}

	/**
	 * A record that passed the text stage, together with what determines its position in the result list.
	 */
	private record ScoredRecord(FLEFRecord record, String displayText, int matchedTerms, double relevance, SearchMatcher.Analysis analysis){}


	private final FLEFModel model;


	public SearchService(final FLEFModel model){
		this.model = model;
	}


	/** The records that pass the filter of the strategy and, if the criteria ask for tags, have all of them. */
	private Predicate<FLEFRecord> withTagFilter(final Predicate<FLEFRecord> filter, final SearchCriteria criteria){
		final List<String> required = RecordTags.parse(criteria.filter(SearchCriteria.FILTER_TAGS));
		return (required.isEmpty()
			? filter
			: record -> filter.test(record) && model.tags().hasAll(record.getId(), required));
	}

	/**
	 * Performs the search according to the given criteria.
	 *
	 * @param criteria the search criteria
	 * @return a list of search results, ordered by match quality
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria){
		return search(criteria, null);
	}

	/**
	 * A record found, with its relevance and the reason why it was found.
	 *
	 * @param record		The record.
	 * @param matchedTerms	How many terms of the query were found in the record, 0 when there is no text query.
	 * @param relevance	The BM25 relevance, 0 when there is no text query.
	 * @param explanation	Html telling which terms were found, where and how relevant the record is; {@code null} when there is no text query.
	 */
	public record SearchHit(FLEFRecord record, int matchedTerms, double relevance, String explanation){}

	/**
	 * Performs the search according to the given criteria and reports progress.
	 *
	 * @param criteria         the search criteria
	 * @param progressCallback consumer for reporting progress percentage (0-100)
	 * @return a list of search results, ordered by match quality
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria, final Consumer<Integer> progressCallback){
		final List<SearchHit> hits = searchExplained(criteria, progressCallback);
		final List<FLEFRecord> results = new ArrayList<>(hits.size());
		for(final SearchHit hit : hits)
			results.add(hit.record());
		return results;
	}

	/**
	 * Performs the search according to the given criteria, and tells why every record was found.
	 * <p>
	 * The text is parsed into terms (words, "exact phrases" and wildcards, see {@link SearchQuery}). The records that pass the
	 * structural filters are the corpus: each is analysed against the terms, the analyses are ranked by {@link Bm25}, and the
	 * records that matched at least one term are returned, sorted by the number of matched terms first (a record matching both
	 * words of a two-word query always ranks above one matching a single word), then by BM25 relevance (a rare word counts more
	 * than a common one, a short text more than a long one, a typo less than an exact word), then by display text.
	 *
	 * @param criteria         the search criteria
	 * @param progressCallback consumer for reporting progress percentage (0-100)
	 * @return the hits, ordered by match quality
	 */
	public List<SearchHit> searchExplained(final SearchCriteria criteria, final Consumer<Integer> progressCallback){
		if(criteria == null)
			return List.of();

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(criteria.recordType());
		final SearchStrategy strategy = REGISTRY.get(handler.getClassType());

		// Stage 1: structural filters (sex, event type, date range, place, ...).
		final Predicate<FLEFRecord> structuralFilter = withTagFilter(strategy.buildPredicate(criteria, model), criteria);

		//the records hidden by the privacy policy are not found
		final List<FLEFRecord> records = PrivacyService.visible(model, model.getRecordsByType(handler.getType()));

		//a group is also found by the names of its members ("Mario Rossi Anna Bianchi" finds the family of the two spouses), and its
		//tooltip lists them
		final GroupMemberNames memberNames = (GroupReader.TYPE.equals(handler.getType())? new GroupMemberNames(model): null);

		// Fast path: no text query. Structural filters only, no ranking.
		final SearchQuery query = SearchQuery.parse(criteria.query());
		if(query.isEmpty()){
			final List<SearchHit> hits = new ArrayList<>();
			for(final FLEFRecord record : executeStructuralSearch(records, structuralFilter, progressCallback))
				hits.add(new SearchHit(record, 0, 0., withMembers(null, record, memberNames)));
			return hits;
		}

		// Stage 2: text analysis and ranking. The display text used for
		// scoring is the one returned by the handler; the enriched display
		// text of the strategy (with the "[M] " prefix and the birth/death
		// details) is only used for the final rendering, not for the
		// text match, so that the behaviour of the search is unchanged.
		final Set<SearchMode> modes = criteria.modes();
		final boolean inNotes = modes.contains(SearchMode.NOTES_AND_CITATIONS);
		final boolean inStories = modes.contains(SearchMode.STORIES);
		//a place is also found by the name of the places that contain it ("Treviso Veneto" finds Treviso, part of Veneto)
		final PlaceNameResolver placeNames = (PlaceReader.TYPE.equals(handler.getType())? new PlaceNameResolver(model): null);
		final List<FLEFRecord> corpusRecords = new ArrayList<>();
		final List<String> displayTexts = new ArrayList<>();
		final List<SearchMatcher.Analysis> analyses = new ArrayList<>();
		final int totalRecords = records.size();
		int lastProgress = -1;
		for(int i = 0; i < totalRecords; i ++){
			if(Thread.currentThread().isInterrupted())
				break;

			final FLEFRecord record = records.get(i);
			if(structuralFilter.test(record)){
				final String displayText = handler.getDisplayText(record, model);
				final List<SearchMatcher.Segment> segments = new ArrayList<>(4);
				segments.add(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, displayText));
				//a record with several names is found by any of them, not only by the one shown
				final List<String> names = allNames(handler.getType(), record);
				if(!names.isEmpty())
					segments.add(new SearchMatcher.Segment(SearchField.OTHER_NAMES, String.join("\n", names)));
				if(placeNames != null)
					segments.add(new SearchMatcher.Segment(SearchField.CONTAINING_PLACES,
						String.join("\n", placeNames.containerNames(record.getId()))));
				if(memberNames != null){
					final List<String> members = memberNames.names(record);
					if(!members.isEmpty())
						segments.add(new SearchMatcher.Segment(SearchField.MEMBER_NAMES, String.join("\n", members)));
				}
				//the free tags of the record are searched like its text
				final List<String> tags = model.tags()
					.get(record.getId());
				if(!tags.isEmpty())
					segments.add(new SearchMatcher.Segment(SearchField.TAGS, String.join(StringUtils.LF, tags)));
				//with the notes included, the text of the notes is matched together with the display text
				if(inNotes)
					segments.add(new SearchMatcher.Segment(SearchField.NOTES_AND_CITATIONS, AttachedTextExtractor.extract(record, model)));
				//the stories are searched on their own option
				if(inStories)
					segments.add(new SearchMatcher.Segment(SearchField.STORIES, AttachedTextExtractor.extractStories(record)));

				corpusRecords.add(record);
				displayTexts.add(displayText);
				analyses.add(SearchMatcher.analyse(query, segments, modes));
			}

			if(progressCallback != null){
				final int progressPercent = (int)(((i + 1) / (double)totalRecords) * 100);
				//notify only when the percentage changes, not at each record
				if(progressPercent != lastProgress){
					lastProgress = progressPercent;
					progressCallback.accept(progressPercent);
				}
			}
		}

		//the statistics of the corpus (how common a word is, how long a text is) come from all the records, not only from the found ones
		final double[] relevance = Bm25.score(analyses);
		final List<ScoredRecord> scored = new ArrayList<>();
		for(int i = 0; i < analyses.size(); i ++){
			final SearchMatcher.Analysis analysis = analyses.get(i);
			final int matched = analysis.matchedTerms();
			if(matched > 0)
				scored.add(new ScoredRecord(corpusRecords.get(i), displayTexts.get(i), matched, relevance[i], analysis));
		}

		// Ranking: number of matched query terms first (a record matching
		// "bort" and "gall" beats a record matching only one of the two),
		// then the BM25 relevance, then display text for a stable,
		// alphabetical tie-break.
		scored.sort(Comparator
			.comparingInt(ScoredRecord::matchedTerms).reversed()
			.thenComparing(Comparator.comparingDouble(ScoredRecord::relevance).reversed())
			.thenComparing(ScoredRecord::displayText, String.CASE_INSENSITIVE_ORDER));

		final List<SearchHit> hits = new ArrayList<>(scored.size());
		for(final ScoredRecord sr : scored)
			hits.add(new SearchHit(sr.record(), sr.matchedTerms(), sr.relevance(), withMembers(explain(sr.analysis(), sr.relevance()), sr.record(), memberNames)));
		return hits;
	}

	/** The most members listed in a tooltip. */
	private static final int MAX_TOOLTIP_MEMBERS = 20;

	/**
	 * @param explanation	The html of the tooltip, or {@code null} if there is none.
	 * @param record			The record found.
	 * @param memberNames	The members of the groups, {@code null} if the records are not groups.
	 * @return	The tooltip with, for a group, the list of its members and their role appended.
	 */
	private static String withMembers(final String explanation, final FLEFRecord record, final GroupMemberNames memberNames){
		if(memberNames == null)
			return explanation;
		final List<GroupMemberNames.Member> members = memberNames.members(record);
		if(members.isEmpty())
			return explanation;

		final StringBuilder sb = new StringBuilder("<html>");
		if(explanation != null)
			sb.append(StringUtils.removeEnd(StringUtils.removeStart(explanation, "<html>"), "</html>"))
				.append("<br>");
		sb.append(I18N.tf("search.tooltip.members", members.size()));
		final int shown = Math.min(members.size(), MAX_TOOLTIP_MEMBERS);
		for(int i = 0; i < shown; i ++){
			final GroupMemberNames.Member member = members.get(i);
			sb.append("<br>&bull; ")
				.append(escapeHtml(member.name()));
			if(member.role() != null)
				sb.append(" (")
					.append(escapeHtml(member.role()))
					.append(')');
		}
		if(members.size() > shown)
			sb.append("<br>").append(I18N.tf("search.tooltip.members_more", members.size() - shown));
		return sb.append("</html>")
			.toString();
	}

	/** @return	Html that tells, for each term of the query, whether and where it was found, and the relevance of the record. */
	private static String explain(final SearchMatcher.Analysis analysis, final double relevance){
		final StringBuilder sb = new StringBuilder("<html>");
		final List<SearchQuery.Term> terms = analysis.query().terms();
		for(int t = 0; t < terms.size(); t ++){
			final String term = escapeHtml(terms.get(t).display());
			if(analysis.best(t) > 0.){
				final List<String> found = new ArrayList<>();
				for(final SearchMatcher.Evidence evidence : analysis.evidence(t))
					found.add(escapeHtml(evidence.text()) + " ("
						+ I18N.t("search.explain.field." + evidence.field().name().toLowerCase(Locale.ROOT)) + ")");
				sb.append(I18N.tf("search.explain.found", term, String.join(", ", found)));
			}
			else
				sb.append(I18N.tf("search.explain.missing", term));
			sb.append("<br>");
		}
		return sb.append(I18N.tf("search.explain.relevance", Math.round(relevance * 100.) / 100.))
			.append("</html>")
			.toString();
	}

	private static String escapeHtml(final String text){
		return StringUtils.replaceEach(text, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

	/** @return	All the names (titles for a source) of a record that can have several of them, otherwise an empty list. */
	private static List<String> allNames(final String type, final FLEFRecord record){
		return switch(type){
			case IndividualReader.TYPE -> IndividualReader.extractFullNames(record);
			case GroupReader.TYPE -> GroupReader.extractNames(record);
			case PlaceReader.TYPE -> PlaceReader.extractNames(record);
			case RepositoryReader.TYPE -> RepositoryReader.extractNames(record);
			case SourceReader.TYPE -> SourceReader.extractTitles(record);
			default -> List.of();
		};
	}

	/**
	 * Structural search without a text query: applies the filters and
	 * preserves the original order of the records.
	 */
	private static List<FLEFRecord> executeStructuralSearch(final List<FLEFRecord> records,
			final Predicate<FLEFRecord> predicate, final Consumer<Integer> progressCallback){
		final List<FLEFRecord> results = new ArrayList<>();
		final int totalRecords = records.size();
		int lastProgress = -1;
		for(int i = 0; i < totalRecords; i ++){
			if(Thread.currentThread().isInterrupted())
				break;

			final FLEFRecord record = records.get(i);
			if(predicate.test(record))
				results.add(record);

			if(progressCallback != null){
				final int progressPercent = (int)(((i + 1) / (double)totalRecords) * 100);
				//notify only when the percentage changes, not at each record
				if(progressPercent != lastProgress){
					lastProgress = progressPercent;
					progressCallback.accept(progressPercent);
				}
			}
		}

		return results;
	}

	public String getDisplayText(final FLEFRecord record){
		if(record == null)
			return null;

		// 1. Resolve the handler for the record's type dynamically.
		// This avoids relying on the 'strategy' field, which is only
		// set during a flat-list search and remains null in hierarchy mode.
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null)
			return record.getId();

		// 2. Look up the corresponding strategy for this handler.
		final SearchStrategy strat = REGISTRY.get(handler.getClass());
		if(strat != null)
			return strat.getDisplayText(record, model);

		// 3. Fallback to the handler's display text if no strategy is found.
		return handler.getDisplayText(record, model);
	}

}
