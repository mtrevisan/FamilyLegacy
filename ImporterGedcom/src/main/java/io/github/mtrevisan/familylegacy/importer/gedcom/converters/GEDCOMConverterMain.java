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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class GEDCOMConverterMain{

	private static final String DEFAULT_INPUT = "Core/src/main/resources/tests/TGMZ.ged";
	private static final String DEFAULT_OUTPUT = "Core/src/main/resources/tests/TGMZ.flef";


	// Usage: java GEDCOMConverterMain [<input.ged> [<output.flef>]]
	static void main(final String[] args) throws IOException, URISyntaxException{
		final String inputResource = (args.length > 0? args[0]: DEFAULT_INPUT);
		final String outputPath = (args.length > 1? args[1]: DEFAULT_OUTPUT);

		// Parse GEDCOM
		final GEDCOMParser parser = new GEDCOMParser();
		final List<GEDCOMNode> roots;
		try(final InputStream is = Files.newInputStream(Paths.get(inputResource));
				final BufferedReader br = GEDCOMHelper.getBufferedReader(is)){
			roots = parser.parse(br);
		}

		// Convert to FLEF
		final GEDCOMToFLEFConverter converter = new GEDCOMToFLEFConverter();
		final FLEFModel model = converter.convert(roots);

		validate(model);

//		FLEFMerger merger = new FLEFMerger(grammar);
//		MergeReport dedupReport = merger.deduplicate(model);

		// Write FLEF file
		final FLEFWriter writer = FLEFWriter.createCompact();
		final Path outputFile = Paths.get(outputPath);
		Files.createDirectories(outputFile.getParent());
		try(final Writer fw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)){
			writer.write(model, fw);
		}

		System.out.println("Conversion completed. Output written to " + outputFile);
	}

	private static void validate(FLEFModel model) throws IOException, URISyntaxException{
		final String resourceName = "/gedg/flef_0.2.0.gedg";
		final var grammarUrl = FLEFGrammar.class.getResource(resourceName);
		if(grammarUrl == null)
			throw new IllegalStateException("Resource " + resourceName + " not found on path module");

		final Path path = Paths.get(grammarUrl.toURI());
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		final List<String> grammarWarnings = grammar.getParseWarnings();
		for(final String warning : grammarWarnings)
			System.err.println(warning);

		final FLEFGrammarValidator.ValidationResult validationResult = FLEFGrammarValidator.validate(grammar);
		final List<String> errors = validationResult.errors();
		for(final String error : errors)
			System.err.println(error);
		final List<String> validationWarnings = validationResult.warnings();
		for(final String warning : validationWarnings)
			System.err.println(warning);

		final FLEFValidator validator = new FLEFValidator(grammar);
		final List<String> errorsSchema = validator.validateSchema(model);
		for(final String error : errorsSchema)
			System.err.println(error);

		final List<String> errorsIntegrity = validator.validateIntegrity(model);
		for(final String error : errorsIntegrity)
			System.err.println(error);
	}

}
