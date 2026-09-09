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
package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class GEDCOMConverterMain{

	// Usage: java GEDCOMConverterMain <input.ged> <output.flef>
	public static void main(String[] args) throws IOException{
		String inputResource = "/tests/TGMZ.ged";
		String outputPath = "src/main/resources/tests/TGMZ.flef";
//		String inputResource = "/tests/TGC55C.ged";
//		String outputPath = "src/main/resources/tests/TGC55C.flef";

		String gedcomContent;
		try(BufferedReader br = GEDCOMHelper.getBufferedReader(GEDCOMToFLEFConverter.class.getResourceAsStream(inputResource))){
			gedcomContent = br.lines()
				.collect(Collectors.joining(System.lineSeparator()));
		}

		// Parse GEDCOM
		GEDCOMParser parser = new GEDCOMParser();
		List<GEDCOMNode> roots;
		try(StringReader reader = new StringReader(gedcomContent)){
			roots = parser.parse(reader);
		}

		// Convert to FLEF
		GEDCOMToFLEFConverter converter = new GEDCOMToFLEFConverter();
		FLEFModel model = converter.convert(roots);

		validate(model);

//		FLEFMerger merger = FLEFMerger.defaultMerger();
//		MergeReport dedupReport = merger.deduplicate(model);

		// Write FLEF file
		FLEFWriter writer = FLEFWriter.createCompact();
		Path outputFile = Paths.get(outputPath);
		Files.createDirectories(outputFile.getParent());
		try(Writer fw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)){
			writer.write(model, fw);
		}

		System.out.println("Conversion completed. Output written to " + outputFile);
	}

	private static void validate(FLEFModel model) throws IOException{
		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.2.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		for(final String warning : grammar.getParseWarnings())
			System.err.println(warning);

		final FLEFGrammarValidator.ValidationResult validationResult = FLEFGrammarValidator.validate(grammar);
		for(final String error : validationResult.errors())
			System.err.println(error);
		for(final String warning : validationResult.warnings())
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
