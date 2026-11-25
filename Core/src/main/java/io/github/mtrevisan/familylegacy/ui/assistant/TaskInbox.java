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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.tools.research.TaskDashboardDialog;

import javax.swing.RootPaneContainer;
import java.awt.Component;
import java.awt.Window;


/** Opens the inbox of the task dashboard, where the tasks that the assistants create wait to be accepted or dismissed. */
final class TaskInbox{

	private TaskInbox(){}


	/**
	 * Opens the inbox over a window (call it on the event dispatch thread); it closes with ESC.
	 *
	 * @param owner the window the inbox is over
	 * @param model the model whose tasks are listed
	 */
	static void open(final Window owner, final FLEFModel model){
		new TaskDashboardDialog(ToolContexts.custom(model, null, anchor(owner), null), TaskBoard.View.INBOX)
			.setVisible(true);
	}

	/** @return the component whose window is the owner (a window is not the ancestor of itself) */
	private static Component anchor(final Window owner){
		return (owner instanceof RootPaneContainer container? container.getRootPane(): owner);
	}

}
