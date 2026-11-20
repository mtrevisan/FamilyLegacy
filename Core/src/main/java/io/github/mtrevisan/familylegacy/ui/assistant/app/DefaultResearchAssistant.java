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
package io.github.mtrevisan.familylegacy.ui.assistant.app;

import io.github.mtrevisan.familylegacy.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.AssistantSettings;
import io.github.mtrevisan.familylegacy.assistant.AssistantStatistics;
import io.github.mtrevisan.familylegacy.assistant.IgnoredFindings;
import io.github.mtrevisan.familylegacy.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.consanguinity.ConsanguinityAssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.fan.FanAssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.source.SourceIdentifierAssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.source.SourceKindAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.DateAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.ResearchAssistantService;
import io.github.mtrevisan.familylegacy.ui.assistant.citation.CitationAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.conclusion.ConclusionAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.context.ContextAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.features.ResearchToolsAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.identity.IdentityAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.place.PlaceAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.planner.PlannerAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.relationship.RelationshipAssistantContributor;

import java.util.ServiceLoader;


/**
 * Assembles the Research Assistant of the application: the service, with the built-in assistants and the ones provided by plugins.
 * <p>
 * This is the only place that knows every assistant, so that the service and the assistants do not depend on one another.
 * </p>
 */
public final class DefaultResearchAssistant{

	private static final ResearchAssistantService INSTANCE = createDefault(IgnoredFindings.usingPreferences(), AssistantSettings.usingPreferences(), true);


	private DefaultResearchAssistant(){}


	/** @return the assistant of the application, that keeps the findings ignored by the user in the preferences */
	public static ResearchAssistantService getInstance(){
		return INSTANCE;
	}

	/**
	 * @param ignoredFindings where the findings ignored by the user are kept ({@link IgnoredFindings#inMemory()} for the tests)
	 * @return a service with the built-in assistants and the ones provided by plugins
	 */
	public static ResearchAssistantService createDefault(final IgnoredFindings ignoredFindings){
		return createDefault(ignoredFindings, AssistantSettings.inMemory());
	}

	/**
	 * @param ignoredFindings where the findings ignored by the user are kept ({@link IgnoredFindings#inMemory()} for the tests)
	 * @param settings what the user decided about the assistant ({@link AssistantSettings#inMemory()} for the tests)
	 * @return a service with the built-in assistants and the ones provided by plugins
	 */
	public static ResearchAssistantService createDefault(final IgnoredFindings ignoredFindings, final AssistantSettings settings){
		return createDefault(ignoredFindings, settings, false);
	}

	/** @param statisticsInFile whether the counts of the user are kept in a file (the application) or only in memory (the tests) */
	private static ResearchAssistantService createDefault(final IgnoredFindings ignoredFindings, final AssistantSettings settings,
			final boolean statisticsInFile){
		final ResearchAssistantService service = new ResearchAssistantService(ignoredFindings, settings,
			(statisticsInFile? AssistantStatistics.usingFile(settings): AssistantStatistics.inMemory(settings::isStatisticsEnabled, settings::tuning)));
		service.register(new DateAssistantContributor());
		service.register(new ChronologyAssistantContributor());
		service.register(new ConclusionAssistantContributor());
		service.register(new RelationshipAssistantContributor());
		service.register(new ConsanguinityAssistantContributor());
		service.register(new FanAssistantContributor());
		service.register(new SourceKindAssistantContributor());
		service.register(new SourceIdentifierAssistantContributor());
		service.register(new IdentityAssistantContributor());
		service.register(new GapAssistantContributor());
		service.register(new CitationAssistantContributor());
		service.register(new PlaceAssistantContributor());
		service.register(new PlannerAssistantContributor());
		service.register(new ContextAssistantContributor());
		service.register(new ResearchToolsAssistantContributor());
		ServiceLoader.load(AssistantContributor.class)
			.forEach(service::register);
		return service;
	}

}
