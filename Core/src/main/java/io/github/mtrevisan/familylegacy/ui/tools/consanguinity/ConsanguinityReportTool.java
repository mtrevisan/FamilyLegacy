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
package io.github.mtrevisan.familylegacy.ui.tools.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;


/**
 * Opens the report of the consanguinity of the file: the couples whose partners are related, and the individuals whose parents are
 * related. It reads the model and never changes it.
 * <p>
 * The report looks at the whole ancestry, so on a big file it can take minutes: it is computed in a background worker, behind a
 * modal progress dialog (the user cannot edit the model meanwhile, so the worker reads it safely), and the event dispatch thread
 * stays free.
 * </p>
 */
public final class ConsanguinityReportTool implements ToolOperation{

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsanguinityReportTool.class);


	@Override
	public String getName(){
		return I18N.t("menu.tools.consanguinity_report");
	}

	@Override
	public void run(final ToolContext context){
		if(context == null || context.model() == null)
			return;

		final FLEFModel model = context.model();
		final ProgressDialog progress = new ProgressDialog(context.owner(), I18N.t("dialog.consanguinity_report.title"),
			I18N.t("progress.consanguinity.computing"));

		final SwingWorker<ConsanguinityReport, Void> worker = new SwingWorker<>(){
			@Override
			protected ConsanguinityReport doInBackground(){
				return ConsanguineousUnionDetector.detect(model);
			}

			@Override
			protected void done(){
				progress.close();
				try{
					final ConsanguinityReport report = get();
					new ConsanguinityReportDialog(context.owner(), model, report, id -> ConsanguineousUnionDetector.nameOf(model, id))
						.setVisible(true);
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ee){
					report(context, ee.getCause() != null? ee.getCause(): ee);
				}
				catch(final RuntimeException | Error ex){
					//the dialog itself failed to be made or shown: said, not left in the log of the thread of the events
					report(context, ex);
				}
			}
		};

		worker.execute();
		//modal: it blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	/** Tells that the report could not be made or shown, and why: a tool that does something and then shows nothing is the worst answer. */
	private static void report(final ToolContext context, final Throwable cause){
		LOGGER.error("Consanguinity report failed", cause);
		JOptionPane.showMessageDialog(context.owner(),
			I18N.t("dialog.consanguinity_report.error") + StringUtils.LF + (cause.getMessage() != null? cause.getMessage(): cause.toString()),
			I18N.t("dialog.consanguinity_report.title"), JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAtLeastIndividuals(2));
	}

}
