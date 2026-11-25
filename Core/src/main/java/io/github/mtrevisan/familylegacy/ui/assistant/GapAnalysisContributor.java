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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.gap.GapAnalysis;
import io.github.mtrevisan.familylegacy.assistant.gap.GapDataLoader;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


/**
 * The Research Gap Analysis: it gathers what is missing from the research (births, deaths, sources, pieces of the chronology,
 * identities to settle) and, for each gap, proposes the research task, the searches to make and the repositories that may keep the
 * record (see {@link GapAnalysis}).
 * <p>
 * It only suggests: a task is created only if the researcher ticks it and confirms, and the searches and the repositories are advice
 * to read.
 * </p>
 */
public final class GapAnalysisContributor implements AssistantContributor{

	private static final Logger LOGGER = LoggerFactory.getLogger(GapAnalysisContributor.class);


	private final Function<AssistantContext, List<Diagnostic>> findings;
	private final Clock clock;


	/** @param findings gives the findings to look at (the ones the user can see in the palette); called away from the event dispatch thread */
	public GapAnalysisContributor(final Function<AssistantContext, List<Diagnostic>> findings){
		this(findings, Clock.systemDefaultZone());
	}

	/** @param clock dates the creation of the tasks (the tests fix it) */
	GapAnalysisContributor(final Function<AssistantContext, List<Diagnostic>> findings, final Clock clock){
		this.findings = findings;
		this.clock = clock;
	}


	@Override
	public List<AssistantAction> actions(){
		return List.of(new GapAnalysisAction());
	}

	/**
	 * @param findings the findings of the assistants
	 * @param model    the model the findings are about
	 * @param today    today
	 * @return the gaps among the findings, each with the task that would start to fill it
	 */
	static List<GapAnalysisDialog.Row> rows(final List<Diagnostic> findings, final FLEFModel model, final LocalDate today){
		final List<GapAnalysis.Entry> entries = GapAnalysis.analyse(findings, GapDataLoader.load(model, today));
		final Set<String> origins = new HashSet<>(TaskBoard.origins(model));
		final List<GapAnalysisDialog.Row> rows = new ArrayList<>(entries.size());
		for(final GapAnalysis.Entry entry : entries){
			final List<TaskProposal> proposals = TaskProposals.propose(List.of(entry.finding()), model, today);
			rows.add(new GapAnalysisDialog.Row(entry, (proposals.isEmpty()? null: proposals.getFirst()),
				entry.finding().key() != null && origins.contains(entry.finding().key())));
		}
		return rows;
	}


	private final class GapAnalysisAction implements AssistantAction{

		@Override
		public String id(){
			return "gaps.analysis";
		}

		@Override
		public AssistantCategory category(){
			return AssistantCategory.RESEARCH;
		}

		@Override
		public String title(){
			return I18N.t("assistant.gaps.action");
		}

		@Override
		public String description(){
			return I18N.t("assistant.gaps.action.description");
		}

		@Override
		public boolean isApplicable(final AssistantContext context){
			return (context.model() != null);
		}

		@Override
		public void execute(final AssistantContext context){
			final FLEFModel model = context.model();
			final Window owner = SwingAssistantContext.owner(context);

			final GapAnalysisDialog dialog = new GapAnalysisDialog(owner, model, clock);
			//the findings are looked at away from the event dispatch thread, on a copy of the model that nobody changes
			final AssistantContext reading = context.forReading();
			CompletableFuture
				.supplyAsync(() -> rows(findings.apply(reading), reading.model(), LocalDate.now()))
				.whenComplete((rows, error) -> SwingUtilities.invokeLater(() -> {
					if(error != null){
						LOGGER.warn("Cannot compute the gaps: {}", error.toString());
						dialog.setFailed();
					}
					else
						dialog.setRows(rows);
				}));
			//what was created is in the inbox of the dashboard (the dialog has a button to open it), to accept or to dismiss
			dialog.setVisible(true);
		}

	}

}
