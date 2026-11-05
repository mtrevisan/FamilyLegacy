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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;


/**
 * The validation of a file, in one pass: what the grammar says (schema, references, rules of the protocol: {@link FLEFValidator}) and
 * what only the graph of the records says (circular parentage, a relationship with itself, an individual without a name, an event
 * nobody took part in: {@link ConsistencyService}) and what only the disk says (a media whose file is not there: {@link MediaFiles}).
 * <p>
 * The validator stops at the first stage that fails, because the stages that follow would only repeat the same cause. The structural
 * checks do not depend on it: they only read the records they understand, and a file that does not follow the grammar can have a circle
 * of parentage too, so they always run. A structural check that cannot be made on a file this broken is a warning, not a failure.
 */
public final class FileValidation{

	/** The share of the work that the grammar is; the structural checks and the files of the media are the rest. */
	private static final double GRAMMAR_SHARE = 0.85;

	private FileValidation(){}


	/**
	 * @param validator	The validator of the grammar.
	 * @param model	The model to validate.
	 * @return	Every issue: the ones of the grammar first, then the structural ones (errors before warnings).
	 */
	public static List<FLEFValidator.ValidationError> run(final FLEFValidator validator, final FLEFModel model){
		return run(validator, model, MediaPathDisplay.baseDirectory());
	}

	/**
	 * @param validator	The validator of the grammar.
	 * @param model	The model to validate.
	 * @param baseDirectory	The directory that the relative locations of the media are relative to ({@code null}: they are not judged).
	 * @return	Every issue: the ones of the grammar first, then the structural ones (errors before warnings).
	 */
	public static List<FLEFValidator.ValidationError> run(final FLEFValidator validator, final FLEFModel model,
			final String baseDirectory){
		return run(validator, model, baseDirectory, fraction -> {});
	}

	/**
	 * The same, telling how far it got, so that the user sees the validation advance: the grammar is the first 70 % of the work (by
	 * records), the structural checks the next 15 %, the files of the media the rest (by files). The progress never goes back, and is
	 * called from the thread that validates.
	 *
	 * @param progress told, from time to time, the share of the work done (0 to 1)
	 */
	public static List<FLEFValidator.ValidationError> run(final FLEFValidator validator, final FLEFModel model, final String baseDirectory,
			final DoubleConsumer progress){
		final List<FLEFValidator.ValidationError> found = new ArrayList<>(validator.validateAllStructured(model,
			fraction -> progress.accept(fraction * GRAMMAR_SHARE)));
		progress.accept(GRAMMAR_SHARE);
		found.addAll(structure(model, baseDirectory, fraction -> progress.accept(GRAMMAR_SHARE + fraction * (1. - GRAMMAR_SHARE))));
		progress.accept(1.);
		return found;
	}

	/** @return	The structural issues, errors first, with the files of the media looked for under the base directory of the user. */
	public static List<FLEFValidator.ValidationError> structure(final FLEFModel model){
		return structure(model, MediaPathDisplay.baseDirectory());
	}

	/** @return	The structural issues, errors first. */
	public static List<FLEFValidator.ValidationError> structure(final FLEFModel model, final String baseDirectory){
		return structure(model, baseDirectory, fraction -> {});
	}

	/** The same, telling how far it got (0 to 1): the checks of the records are the first half, the files of the media the second. */
	public static List<FLEFValidator.ValidationError> structure(final FLEFModel model, final String baseDirectory,
			final DoubleConsumer progress){
		if(model == null)
			return List.of();

		final List<FLEFValidator.ValidationError> found = new ArrayList<>();
		try{
			final List<ConsistencyService.Issue> issues = new ArrayList<>(ConsistencyService.check(model).issues());
			progress.accept(0.5);
			issues.addAll(MediaFiles.missing(model, baseDirectory, fraction -> progress.accept(0.5 + fraction / 2.)));
			for(final ConsistencyService.Issue issue : issues)
				found.add(new FLEFValidator.ValidationError(issue.message(), issue.recordId(), (issue.severity() == ConsistencyService.Severity.ERROR
					? FLEFValidator.Severity.ERROR
					: FLEFValidator.Severity.WARNING), issue.kind()));
		}
		catch(final RuntimeException re){
			found.add(new FLEFValidator.ValidationError(I18N.tf("text.consistency.issue.incomplete", re.toString()), null,
				FLEFValidator.Severity.WARNING, "incomplete"));
		}
		found.sort((a, b) -> a.severity().compareTo(b.severity()) * -1);
		return found;
	}

}
