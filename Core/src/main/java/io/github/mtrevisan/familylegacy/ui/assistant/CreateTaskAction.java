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

import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.assistant.task.TaskTopic;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import javax.swing.JOptionPane;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;


/**
 * "Create Research Task": turns one finding into a research task at once, without the review list of
 * <em>Generate Research Tasks from Findings</em> (the researcher chose this finding and this action). The task goes to the inbox of the
 * task dashboard; a finding that already became a task is not made one twice.
 */
final class CreateTaskAction implements DiagnosticAction{

	private final Diagnostic diagnostic;
	private final int slot;
	private final Clock clock;


	/**
	 * @param diagnostic the finding
	 * @param slot       the key of the action in the palette (see {@link DiagnosticAction#keySlot()})
	 */
	CreateTaskAction(final Diagnostic diagnostic, final int slot){
		this(diagnostic, slot, Clock.systemDefaultZone());
	}

	CreateTaskAction(final Diagnostic diagnostic, final int slot, final Clock clock){
		this.diagnostic = diagnostic;
		this.slot = slot;
		this.clock = clock;
	}


	/** @return whether the finding asks for work that can become a task */
	static boolean isApplicable(final Diagnostic diagnostic){
		return (diagnostic.key() != null && TaskTopic.of(diagnostic.kind()) != null);
	}

	@Override
	public String title(){
		return I18N.t("assistant.tasks.create_one");
	}

	@Override
	public int keySlot(){
		return slot;
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		final List<TaskProposal> proposals = TaskProposals.propose(List.of(diagnostic), model, LocalDate.now());
		final String message;
		if(proposals.isEmpty())
			message = I18N.t("assistant.tasks.exists");
		else{
			TaskProposals.create(model, proposals.getFirst(), clock);
			message = I18N.tf("assistant.tasks.created", proposals.getFirst().description());
		}
		JOptionPane.showMessageDialog(SwingAssistantContext.owner(context), message, title(), JOptionPane.INFORMATION_MESSAGE);
	}

}
