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
package io.github.mtrevisan.familylegacy.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Lazy holder of the grammar used by the merge tools: to plan and apply a merge, to validate its result, and to drive the
 * autocomplete.
 * <p>
 * The grammar file is fixed and never changes at runtime, so it is parsed once and shared. It is read from the classpath, and
 * from the source tree when running from the IDE without resources.
 */
public final class MergeGrammar{

	private static final Logger LOGGER = LoggerFactory.getLogger(MergeGrammar.class);

	private static final String GRAMMAR_CLASSPATH = "/gedg/flef_0.2.0.gedg";
	private static final String GRAMMAR_FILE = "src/main/resources/gedg/flef_0.2.0.gedg";

	private static final FLEFGrammar INSTANCE = load();


	private MergeGrammar(){}


	/**
	 * @return the grammar, or {@code null} if it cannot be read
	 */
	public static FLEFGrammar instance(){
		return INSTANCE;
	}

	private static FLEFGrammar load(){
		try(final InputStream is = MergeGrammar.class.getResourceAsStream(GRAMMAR_CLASSPATH)){
			if(is != null)
				return FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));

			final Path path = Paths.get(GRAMMAR_FILE);
			if(Files.exists(path))
				return FLEFGrammarParser.parse(path);
		}
		catch(final Exception ex){
			LOGGER.warn("Unable to load the FLEF grammar", ex);
			return null;
		}

		LOGGER.warn("FLEF grammar not found: merging is disabled, autocomplete falls back to the hardcoded table, schema"
			+ " validation is skipped");
		return null;
	}

}
