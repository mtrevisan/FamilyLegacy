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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.quality.FileValidation;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;


/**
 * Runs the full validation of the current model (the grammar of FLEF, and the structural checks of {@link FileValidation}: circular
 * parentage, a relationship with itself, an individual without a name) and shows the
 * report dialog. The validation runs in a background worker so even a
 * large model does not freeze the UI.
 * <p>
 * The report dialog receives the structured {@link
 * FLEFValidator.ValidationError} objects, each carrying both the
 * message and, when applicable, the id of the record the issue is
 * about. The dialog therefore does not need to parse the message.
 */
public final class ValidateFileTool implements ToolOperation{

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidateFileTool.class);


	private static volatile FLEFGrammar cachedGrammar;
	private static volatile boolean grammarLoaded;


	@Override
	public String getName(){
		return I18N.t("menu.tools.validate");
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final FLEFModel model = context.model();
		if(model == null){
			JOptionPane.showMessageDialog(context.owner(),
				I18N.t("message.no_model_loaded"), I18N.t("dialog.validate.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		final FLEFGrammar grammar = grammar();
		if(grammar == null){
			JOptionPane.showMessageDialog(context.owner(),
				I18N.t("dialog.validate.no_grammar"), I18N.t("dialog.validate.title"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		//the report opens at once, with a progress bar, and validates in the background: the model is read by another thread while the
		//user may be editing it, so a copy that nobody changes is read (the dialog takes a new one for every validation)
		final FLEFValidator validator = new FLEFValidator(grammar);
		ValidationReportDialog.validating(context.owner(), model, context::readOnlyModel,
				(view, progress) -> FileValidation.run(validator, view, MediaPathDisplay.baseDirectory(), progress))
			.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}


	/* ======================================================================
	 *                          Grammar loading
	 * ====================================================================== */

	private static FLEFGrammar grammar(){
		if(!grammarLoaded){
			synchronized(ValidateFileTool.class){
				if(!grammarLoaded){
					cachedGrammar = loadGrammar();
					grammarLoaded = true;
				}
			}
		}
		return cachedGrammar;
	}

	private static FLEFGrammar loadGrammar(){
		final FLEFGrammar grammar = MergeGrammar.instance();
		if(grammar == null)
			LOGGER.error("Grammar not found: validation is disabled");
		return grammar;
	}

}
