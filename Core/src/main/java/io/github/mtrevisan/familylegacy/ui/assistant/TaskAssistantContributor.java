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
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


/**
 * The Research Task Assistant: it turns the findings of the other assistants into persistent research tasks, that the researcher
 * reviews before they are created and then follows in the task dashboard (inbox, filters, completion).
 * <p>
 * It never creates a task on its own: <em>Generate Research Tasks from Findings</em> lists what the findings ask for, and only the
 * tasks the researcher ticks and confirms are written to the file. A finding that became a task is not proposed again, whatever happens
 * to the task.
 * </p>
 */
public final class TaskAssistantContributor implements AssistantContributor{

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskAssistantContributor.class);


	private final Function<AssistantContext, List<Diagnostic>> findings;
	private final Clock clock;


	/** @param findings gives the findings to turn into tasks (the ones the user can see in the palette); called away from the event dispatch thread */
	public TaskAssistantContributor(final Function<AssistantContext, List<Diagnostic>> findings){
		this(findings, Clock.systemDefaultZone());
	}

	/** @param clock dates the creation of the tasks (the tests fix it) */
	TaskAssistantContributor(final Function<AssistantContext, List<Diagnostic>> findings, final Clock clock){
		this.findings = findings;
		this.clock = clock;
	}


	@Override
	public List<AssistantAction> actions(){
		return List.of(new GenerateTasksAction());
	}


	private final class GenerateTasksAction implements AssistantAction{

		@Override
		public String id(){
			return "tasks.generate";
		}

		@Override
		public AssistantCategory category(){
			return AssistantCategory.RESEARCH;
		}

		@Override
		public String title(){
			return I18N.t("assistant.tasks.generate");
		}

		@Override
		public String description(){
			return I18N.t("assistant.tasks.generate.description");
		}

		@Override
		public boolean isApplicable(final AssistantContext context){
			return (context.model() != null);
		}

		@Override
		public void execute(final AssistantContext context){
			final FLEFModel model = context.model();
			final Window owner = SwingAssistantContext.owner(context);

			final GenerateTasksDialog dialog = new GenerateTasksDialog(owner, model, clock);
			//the findings are looked at away from the event dispatch thread, on a copy of the model that nobody changes
			final AssistantContext reading = context.forReading();
			CompletableFuture
				.supplyAsync(() -> TaskProposals.propose(findings.apply(reading), reading.model(), LocalDate.now()))
				.whenComplete((proposals, error) -> SwingUtilities.invokeLater(() -> {
					if(error != null){
						LOGGER.warn("Cannot compute the findings: {}", error.toString());
						dialog.setFailed();
					}
					else
						dialog.setProposals(proposals);
				}));
			//what was created is in the inbox of the dashboard (the dialog has a button to open it), to accept or to dismiss
			dialog.setVisible(true);
		}

	}

}
