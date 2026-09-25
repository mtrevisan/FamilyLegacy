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
package io.github.mtrevisan.familylegacy.v2.ui.tools.validate;

import io.github.mtrevisan.familylegacy.v2.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Runs the full FLEF validation on the current model and shows the
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

	private static final String GRAMMAR_CLASSPATH = "/gedg/flef_0.1.2.gedg";
	private static final String GRAMMAR_FILE = "src/main/resources/gedg/flef_0.1.2.gedg";

	private static volatile FLEFGrammar cachedGrammar;
	private static volatile boolean grammarLoaded;


	@Override
	public String getName(){
		return "Validate File…";
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final FLEFModel model = context.model();
		if(model == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No model loaded.", "Validate File", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final FLEFGrammar grammar = grammar();
		if(grammar == null){
			JOptionPane.showMessageDialog(context.owner(),
				"Grammar file not found: cannot validate.",
				"Validate File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final SwingWorker<List<FLEFValidator.ValidationError>, Void> worker = new SwingWorker<>(){
			private final FLEFValidator validator = new FLEFValidator(grammar);

			@Override
			protected List<FLEFValidator.ValidationError> doInBackground(){
				return validator.validateAllStructured(model);
			}

			@Override
			protected void done(){
				try{
					final List<FLEFValidator.ValidationError> errors = get();
					new ValidationReportDialog(context.owner(), model,
						() -> validator.validateAllStructured(model))
						.setVisible(true);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					LOGGER.error("Validation failed", ex);
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					JOptionPane.showMessageDialog(context.owner(),
						"Validation failed:\n" + cause.getMessage(),
						"Validate File", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		worker.execute();
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
		try(final InputStream is = ValidateFileTool.class
			.getResourceAsStream(GRAMMAR_CLASSPATH)){
			if(is != null){
				LOGGER.info("Grammar loaded from classpath {}", GRAMMAR_CLASSPATH);
				return FLEFGrammarParser.parse(
					new String(is.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		catch(final IOException ex){
			LOGGER.warn("Classpath grammar read failed: {}", ex.getMessage());
		}

		try{
			final Path path = Paths.get(GRAMMAR_FILE);
			if(Files.exists(path)){
				LOGGER.info("Grammar loaded from file {}", path.toAbsolutePath());
				return FLEFGrammarParser.parse(path);
			}
		}
		catch(final Exception ex){
			LOGGER.error("Unable to load grammar from {}", GRAMMAR_FILE, ex);
		}

		LOGGER.error("Grammar not found: validation is disabled");
		return null;
	}

}
