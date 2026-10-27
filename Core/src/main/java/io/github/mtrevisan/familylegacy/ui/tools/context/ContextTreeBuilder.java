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
package io.github.mtrevisan.familylegacy.ui.tools.context;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchTreeBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


/**
 * Builds the {@code context -> impact} tree shown by {@link ContextBoardDialog}.
 * <p>
 * There are two sections, <em>Historic events</em> and <em>Cultural norms</em>; under each context (sorted by title) are the
 * context impacts that cite it, labelled with the affected target and the impact type. Impacts whose context cannot be
 * found are collected under a synthetic {@link Kind#ORPHANS} node, so nothing is silently dropped. An impact that cites
 * more than one context appears under each of them.
 * <p>
 * The class holds no Swing state, so it can be tested on a plain model.
 */
public final class ContextTreeBuilder{

	/** The kind of a tree node. */
	public enum Kind{
		/** Synthetic parent of the historic events. */
		HISTORIC_EVENTS("H"),
		/** Synthetic parent of the cultural norms. */
		CULTURAL_NORMS("N"),
		HISTORIC_EVENT("H"),
		CULTURAL_NORM("N"),
		IMPACT("I"),
		/** Synthetic parent of the impacts that cite no existing context. */
		ORPHANS("?");


		private final String symbol;


		Kind(final String symbol){
			this.symbol = symbol;
		}

		public String symbol(){
			return symbol;
		}

		/** Whether the node is a context (a historic event or a cultural norm). */
		public boolean isContext(){
			return (this == HISTORIC_EVENT || this == CULTURAL_NORM);
		}
	}


	/** A node of the context tree. */
	public static final class Node{

		private final Kind kind;
		private final FLEFRecord record;
		private final String label;
		private final List<Node> children = new ArrayList<>();


		Node(final Kind kind, final FLEFRecord record, final String label){
			this.kind = kind;
			this.record = record;
			this.label = label;
		}

		public Kind kind(){
			return kind;
		}

		/** The underlying record, {@code null} for the synthetic nodes. */
		public FLEFRecord record(){
			return record;
		}

		public String label(){
			return label;
		}

		public List<Node> children(){
			return children;
		}

		/**
		 * Returns a copy of the subtree that keeps only the branches whose label (or a descendant's) contains the text. A
		 * node whose own label matches is kept with all its children.
		 *
		 * @param needle	The text to look for, case-insensitive.
		 * @return	The pruned copy, or {@code null} if nothing in the subtree matches.
		 */
		public Node filter(final String needle){
			if(StringUtils.isBlank(needle))
				return this;

			return filterLowerCase(needle.trim()
				.toLowerCase(Locale.ROOT));
		}

		private Node filterLowerCase(final String needle){
			if(label.toLowerCase(Locale.ROOT).contains(needle))
				return this;

			final Node copy = new Node(kind, record, label);
			for(final Node child : children){
				final Node filtered = child.filterLowerCase(needle);
				if(filtered != null)
					copy.children.add(filtered);
			}
			return (!copy.children.isEmpty()? copy: null);
		}

		@Override
		public String toString(){
			return label;
		}

	}


	private static final int MAX_LABEL_LENGTH = 90;


	private ContextTreeBuilder(){}


	/**
	 * Builds the context forest.
	 *
	 * @param model	The model to read from.
	 * @return	The root nodes: the sections that are not empty, plus an {@link Kind#ORPHANS} node when some impact cites no
	 * 	existing context.
	 */
	public static List<Node> build(final FLEFModel model){
		final List<FLEFRecord> events = sortedByTitle(PrivacyService.visible(model, model.getRecordsByType(HistoricEventHandler.TYPE)),
			HistoricEventReader::extractTitle);
		final List<FLEFRecord> norms = sortedByTitle(PrivacyService.visible(model, model.getRecordsByType(CulturalNormHandler.TYPE)),
			CulturalNormReader::extractTitle);
		final List<FLEFRecord> impacts = PrivacyService.visible(model, model.getRecordsByType(ContextImpactHandler.TYPE));

		final Map<String, List<FLEFRecord>> impactsByContext = new HashMap<>();
		for(final FLEFRecord impact : impacts)
			for(final String contextId : ResearchTreeBuilder.references(impact, ContextImpactReader.TAG_CONTEXT))
				impactsByContext.computeIfAbsent(contextId, k -> new ArrayList<>())
					.add(impact);

		final Set<String> placed = new HashSet<>();
		final List<Node> roots = new ArrayList<>();

		final Node eventsNode = new Node(Kind.HISTORIC_EVENTS, null, "Historic events (" + events.size() + ")");
		for(final FLEFRecord event : events)
			eventsNode.children.add(contextNode(Kind.HISTORIC_EVENT, event, historicEventLabel(event), impactsByContext, model,
				placed));
		if(!events.isEmpty())
			roots.add(eventsNode);

		final Node normsNode = new Node(Kind.CULTURAL_NORMS, null, "Cultural norms (" + norms.size() + ")");
		for(final FLEFRecord norm : norms)
			normsNode.children.add(contextNode(Kind.CULTURAL_NORM, norm, culturalNormLabel(norm), impactsByContext, model,
				placed));
		if(!norms.isEmpty())
			roots.add(normsNode);

		final Node orphans = new Node(Kind.ORPHANS, null, "(no context)");
		for(final FLEFRecord impact : impacts)
			if(!placed.contains(impact.getId()))
				orphans.children.add(new Node(Kind.IMPACT, impact, impactLabel(impact, model)));
		if(!orphans.children.isEmpty())
			roots.add(orphans);

		return roots;
	}

	private static Node contextNode(final Kind kind, final FLEFRecord context, final String label,
			final Map<String, List<FLEFRecord>> impactsByContext, final FLEFModel model, final Set<String> placed){
		final List<FLEFRecord> contextImpacts = impactsByContext.getOrDefault(context.getId(), List.of());

		final Node node = new Node(kind, context, label + " (" + contextImpacts.size() + "I)");
		for(final FLEFRecord impact : contextImpacts){
			node.children.add(new Node(Kind.IMPACT, impact, impactLabel(impact, model)));
			placed.add(impact.getId());
		}
		return node;
	}

	private static List<FLEFRecord> sortedByTitle(final List<FLEFRecord> records, final Function<FLEFRecord, String> title){
		final List<FLEFRecord> sorted = new ArrayList<>(records);
		sorted.sort(Comparator.comparing((FLEFRecord record) -> StringUtils.defaultString(title.apply(record))
				.toLowerCase(Locale.ROOT))
			.thenComparing(FLEFRecord::getId));
		return sorted;
	}


	/* ======================================================================
	 *                          Labels
	 * ====================================================================== */

	private static String historicEventLabel(final FLEFRecord event){
		final String title = HistoricEventReader.extractTitle(event);
		return withDetails((StringUtils.isNotBlank(title)? title: event.getId()), HistoricEventReader.extractType(event),
			ResearchHelper.firstTextValue(event, HistoricEventReader.TAG_DATE));
	}

	private static String culturalNormLabel(final FLEFRecord norm){
		final String title = CulturalNormReader.extractTitle(norm);
		final String from = ResearchHelper.firstTextValue(norm, CulturalNormReader.TAG_VALID_FROM);
		final String to = ResearchHelper.firstTextValue(norm, CulturalNormReader.TAG_VALID_TO);
		final String validity = (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(to)
			? StringUtils.defaultString(from) + "–" + StringUtils.defaultString(to)
			: null);
		return withDetails((StringUtils.isNotBlank(title)? title: norm.getId()), CulturalNormReader.extractType(norm), validity);
	}

	private static String impactLabel(final FLEFRecord impact, final FLEFModel model){
		final String target = targetText(impact, model);
		return withDetails("→ " + (StringUtils.isNotBlank(target)? target: "(no target)"),
			ContextImpactReader.extractImpactType(impact),
			ResearchHelper.firstTextValue(impact, ContextImpactReader.TAG_CONFIDENCE));
	}

	/** The display text of the record the impact targets, or the referenced id when the record is missing. */
	private static String targetText(final FLEFRecord impact, final FLEFModel model){
		final FLEFRecord target = FLEFRecordHelper.findChild(impact, ContextImpactReader.TAG_TARGET);
		if(target == null || target.getChildren().isEmpty())
			return null;

		final FLEFRecord reference = target.getChildren()
			.getFirst();
		final String id = reference.getValue();
		if(StringUtils.isBlank(id))
			return null;

		final FLEFRecord targetRecord = model.getRecordById(id);
		final RecordTypeHandler<?> handler = (targetRecord != null? HandlerRegistry.getHandler(reference.getTag()): null);
		return (handler != null? handler.getDisplayText(targetRecord, model): id);
	}

	private static String withDetails(final String text, final String... details){
		final List<String> parts = new ArrayList<>();
		for(final String detail : details)
			if(StringUtils.isNotBlank(detail))
				parts.add(detail);
		final String abbreviated = StringUtils.abbreviate(StringUtils.normalizeSpace(text), MAX_LABEL_LENGTH);
		return (parts.isEmpty()? abbreviated: abbreviated + " [" + String.join(", ", parts) + "]");
	}

}
