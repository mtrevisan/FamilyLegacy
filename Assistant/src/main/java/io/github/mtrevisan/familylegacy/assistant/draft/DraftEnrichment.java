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
package io.github.mtrevisan.familylegacy.assistant.draft;

import io.github.mtrevisan.familylegacy.assistant.Sentences;
import io.github.mtrevisan.familylegacy.assistant.gap.GapData;
import io.github.mtrevisan.familylegacy.assistant.gap.GapDataLoader;
import io.github.mtrevisan.familylegacy.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityDraft;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * The step shared by the drafts of the Research Gap Assistant, of the Research Planner and of the Identity Assistant: it completes a
 * plain draft with what the tree already knows, just before the editor opens.
 * <ul>
 *   <li>where to look: the repositories placed at the places of the individuals (or at the places that contain them), and the sources
 *   already cited by their relatives (parents, children and partners), that may name them too;</li>
 *   <li>a due date for a task, that depends on its priority;</li>
 *   <li>the task that would follow a question, to plan once the question is saved.</li>
 * </ul>
 * It only suggests: everything ends in editable fields of an editor, and nothing is saved unless the researcher confirms.
 */
public final class DraftEnrichment{

	/** How many repositories, or sources, are named in a draft. */
	static final int MAX_NAMED = 3;
	/** The days from today a task is due, by priority. */
	static final int DUE_DAYS_HIGH = 14;
	static final int DUE_DAYS_NORMAL = 30;
	static final int DUE_DAYS_LOW = 90;


	private DraftEnrichment(){}


	/**
	 * @param model the model
	 * @param draft the plain draft of a question or of a task
	 * @param today today
	 * @return the draft with the places to look at, the due date of a task and the task that would follow a question
	 */
	public static GapDraft enrich(final FLEFModel model, final GapDraft draft, final LocalDate today){
		final GapData data = GapDataLoader.load(model, today);
		final Set<String> persons = personsOf(draft.records(), data);
		return enrich(draft, repositoriesOf(persons, data), sourceTitles(model, relativeSources(model, persons)), today);
	}

	/**
	 * @param model the model
	 * @param draft the plain draft of a hypothesis
	 * @return the draft with where to look for the evidence that would decide it
	 */
	public static IdentityDraft enrich(final FLEFModel model, final IdentityDraft draft){
		final GapData data = GapDataLoader.load(model, LocalDate.now());
		final Set<String> persons = new LinkedHashSet<>(List.of(draft.leftId(), draft.rightId()));
		final List<String> sources = relativeSources(model, persons).stream()
			.filter(source -> !draft.sharedSourceIds().contains(source))
			.toList();
		return enrich(draft, repositoriesOf(persons, data), sourceTitles(model, sources));
	}


	static GapDraft enrich(final GapDraft draft, final List<String> repositories, final List<String> relativeSources,
			final LocalDate today){
		String text = draft.text();
		if(!repositories.isEmpty() && repositories.stream().noneMatch(text::contains))
			text = Sentences.join(text, I18N.tf("draft.enrichment.repositories", named(repositories)));
		if(!relativeSources.isEmpty())
			text = Sentences.join(text, I18N.tf("draft.enrichment.relative_sources", named(relativeSources)));

		if(draft.kind() == GapDraft.Kind.TASK)
			return new GapDraft(draft.kind(), draft.title(), text, draft.records(), draft.priority(),
				(draft.dueDate() != null? draft.dueDate(): dueDate(draft.priority(), today)), draft.followUp());

		String followUp = I18N.tf("draft.enrichment.follow_up", draft.title());
		if(!repositories.isEmpty())
			followUp = Sentences.join(followUp, I18N.tf("draft.enrichment.repositories", named(repositories)));
		return new GapDraft(draft.kind(), draft.title(), text, draft.records(), draft.priority(), draft.dueDate(),
			(draft.followUp() != null? draft.followUp(): followUp));
	}

	static IdentityDraft enrich(final IdentityDraft draft, final List<String> repositories, final List<String> relativeSources){
		String rationale = draft.rationale();
		if(!repositories.isEmpty())
			rationale = Sentences.join(rationale, I18N.tf("draft.enrichment.repositories", named(repositories)));
		if(!relativeSources.isEmpty())
			rationale = Sentences.join(rationale, I18N.tf("draft.enrichment.relative_sources", named(relativeSources)));

		return new IdentityDraft(draft.leftId(), draft.rightId(), rationale, draft.confidence(), draft.similarities(),
			draft.differences(), draft.sharedSourceIds());
	}

	/** @return the day a task of that priority is due, as an ISO date */
	public static String dueDate(final String priority, final LocalDate today){
		final int days = (ResearchTaskReader.ENUM_PRIORITY_HIGH.equals(priority)? DUE_DAYS_HIGH
			: (ResearchTaskReader.ENUM_PRIORITY_LOW.equals(priority)? DUE_DAYS_LOW: DUE_DAYS_NORMAL));
		return today.plusDays(days).toString();
	}

	private static String named(final List<String> names){
		return Sentences.list(names.subList(0, Math.min(MAX_NAMED, names.size())));
	}

	/** @return the individuals a draft is about: the individuals, the principals of its events, the individuals targeted by its questions */
	private static Set<String> personsOf(final List<String> records, final GapData data){
		final Set<String> persons = new LinkedHashSet<>();
		for(final String id : records){
			if(data.persons().containsKey(id))
				persons.add(id);
			data.events().stream()
				.filter(event -> event.id().equals(id))
				.forEach(event -> persons.addAll(event.principals()));
			data.questions().stream()
				.filter(question -> question.id().equals(id))
				.forEach(question -> question.targets().stream()
					.filter(data.persons()::containsKey)
					.forEach(persons::add));
		}
		return persons;
	}

	private static List<String> repositoriesOf(final Set<String> persons, final GapData data){
		final Set<String> repositories = new TreeSet<>();
		for(final String id : persons){
			final GapData.Person person = data.persons().get(id);
			if(person != null)
				repositories.addAll(person.repositories());
		}
		return List.copyOf(repositories);
	}

	/**
	 * @return the sources cited by the parents, the children and the partners of the individuals, and not by the individuals themselves:
	 * where the relatives were found is where the individuals may be found too (identifiers)
	 */
	private static List<String> relativeSources(final FLEFModel model, final Set<String> persons){
		final Set<String> relatives = new LinkedHashSet<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			final String type = RelationshipReader.extractType(relationship);
			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String target = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(type == null || subject == null || target == null || !(RelationshipReader.isTypeChild(type) || RelationshipReader.isTypePartner(type)))
				continue;

			if(persons.contains(subject) && !persons.contains(target))
				relatives.add(target);
			else if(persons.contains(target) && !persons.contains(subject))
				relatives.add(subject);
		}

		final Set<String> own = new LinkedHashSet<>();
		for(final String id : persons)
			own.addAll(sourcesOf(model.getRecordById(id)));
		final Set<String> sources = new TreeSet<>();
		for(final String relative : relatives)
			for(final String source : sourcesOf(model.getRecordById(relative)))
				if(!own.contains(source))
					sources.add(source);
		return List.copyOf(sources);
	}

	private static Set<String> sourcesOf(final FLEFRecord individual){
		final Set<String> sources = new LinkedHashSet<>();
		if(individual != null)
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_SOURCE)){
				final String id = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE);
				if(id != null)
					sources.add(id);
			}
		return sources;
	}

	private static List<String> sourceTitles(final FLEFModel model, final List<String> sourceIds){
		final List<String> titles = new ArrayList<>();
		for(final String id : sourceIds){
			final FLEFRecord source = model.getRecordById(id);
			titles.add(source != null? SourceReader.displayText(source): "[" + id + "]");
		}
		return titles;
	}

}
