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
package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationError;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationException;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * Manages reading and writing of FLAG (.flef) data files.
 * <p>
 * This class provides both plain loading (without validation) and
 * integrated loading with validation against a grammar file.
 */
public final class FLEFFile{

	private FLEFFile(){}


	/**
	 * Loads a FLEF data file and validates it against a grammar file.
	 *
	 * @param flefPath path to the .flef data file
	 * @param gedgPath path to the .gedg protocol file
	 * @return validated FLEFModel
	 * @throws IOException	If files cannot be read
	 * @throws ValidationException	If validation fails
	 */
	public static FLEFModel loadAndValidate(final Path flefPath, final Path gedgPath) throws IOException,
			ValidationException{
		final FLEFGrammar grammar = FLEFGrammar.createFromPath(gedgPath);
		return loadWithGrammar(flefPath, grammar);
	}

	/**
	 * Loads a FLEF data file and validates it against a pre-loaded grammar.
	 *
	 * @param flefFilePath	Path to the .flef data file
	 * @param grammar	The grammar to validate against
	 * @return	Validated FLEFModel
	 * @throws IOException	If the file cannot be read
	 * @throws ValidationException	If validation fails
	 */
	public static FLEFModel loadWithGrammar(final String flefFilePath, final FLEFGrammar grammar) throws IOException,
			ValidationException{
		return loadWithGrammar(Path.of(flefFilePath), grammar);
	}

	/**
	 * Loads a FLEF data file and validates it against a pre-loaded grammar.
	 *
	 * @param flefPath	Path to the .flef data file
	 * @param grammar	The grammar to validate against
	 * @return	Validated FLEFModel
	 * @throws IOException	If the file cannot be read
	 * @throws ValidationException	If validation fails
	 */
	public static FLEFModel loadWithGrammar(final Path flefPath, final FLEFGrammar grammar) throws IOException,
			ValidationException{
		final FLEFModel model = load(flefPath);

		final FLEFValidator validator = FLEFValidator.create(grammar);
		final List<ValidationError> errors = validator.validate(model);
		if(!errors.isEmpty())
			throw ValidationException.create(errors);

		return model;
	}

	/**
	 * Loads a FLEF file and returns a FLEFModel without validation.
	 *
	 * @param filePath file path
	 * @return populated model
	 * @throws IOException if the file cannot be read
	 */
	public static FLEFModel load(final Path filePath) throws IOException{
		final FLEFModel model = new FLEFModel();
		final List<String> lines = readLines(filePath);

		int index = 0;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(isHeaderLine(line)){
				final FLEFRecord header = parseHeader(lines, index);
				model.setHeader(header);
				index += header.getLineCount();
			}
			else if(isRecordLine(line)){
				final FLEFRecord record = parseRecord(lines, index);
				model.addRecord(record);
				index += record.getLineCount();
			}
			else if(isEndOfFileLine(line))
				// EOF marker
				break;
			else
				index ++;
		}

		return model;
	}

	/**
	 * Saves a FLEF model to a file.
	 *
	 * @param model    The model to save
	 * @param filePath The file path
	 * @throws IOException if the file cannot be written
	 */
	public static void save(final FLEFModel model, final String filePath) throws IOException{
		final StringBuilder sb = convertToString(model);

		try(final BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))){
			writer.write(sb.toString());
		}
	}

	/**
	 * Print a FLEF model to standard output.
	 *
	 * @param model    The model to save
	 */
	public static void print(final FLEFModel model){
		final StringBuilder sb = convertToString(model);

		System.out.println(sb);
	}

	private static StringBuilder convertToString(final FLEFModel model){
		final StringBuilder sb = new StringBuilder();

		// Header
		if(model.getHeader() != null)
			sb.append(serializeRecord(model.getHeader(), 0));

		// Records
		for(final FLEFRecord record : model.getRecords())
			sb.append(serializeRecord(record, 0));

		// EOF
		sb.append("0 EOF\n");
		return sb;
	}

	/**
	 * Read all lines of a file.
	 */
	private static List<String> readLines(final Path filePath) throws IOException{
		final List<String> lines = new ArrayList<>();
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))){
			String line;
			while((line = reader.readLine()) != null){
				if(!line.trim().isEmpty())
					lines.add(line.trim());
			}
		}
		return lines;
	}

	/**
	 * Checks whether a line is the start of the header.
	 */
	private static boolean isHeaderLine(final String line){
		return line.startsWith("0 HEADER");
	}

	/**
	 * Checks whether a line is the start of a record (e.g. 0 @I1@ INDIVIDUAL).
	 */
	private static boolean isRecordLine(final String line){
		return line.matches("0 @[^@]+@ [A-Z_]+");
	}

	/**
	 * Checks if a line is the EOF marker.
	 */
	private static boolean isEndOfFileLine(final String line){
		return line.equals("0 EOF");
	}

	/**
	 * Header parser.
	 */
	private static FLEFRecord parseHeader(final List<String> lines, final int startIndex){
		final FLEFRecord record = new FLEFRecord();
		record.setType("HEADER");

		int index = startIndex + 1;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(line.startsWith("0 "))
				// Start of a new record or EOF
				break;

			// Parsing a header line (e.g., "1 PROTOCOL", "2 NAME FLEF")
			final FLEFRecord child = parseChildLine(line);
			record.addChild(child);

			// If the child has children, we add them
			if(child.hasChildren()){
				// The child's children have already been added during parsing.
			}

			index ++;
		}

		// Save the number of parsed lines
		record.setLineCount(index - startIndex);
		return record;
	}

	/**
	 * Parser of a generic record (individual, family, place, etc.).
	 */
	private static FLEFRecord parseRecord(final List<String> lines, final int startIndex){
		final String firstLine = lines.get(startIndex);
		// Example: "0 @I1@ INDIVIDUAL"
		final String[] parts = firstLine.split(" ", 3);
		final String id = parts[1].substring(1, parts[1].length() - 1); // rimuove @
		final String type = parts[2];

		final FLEFRecord record = FLEFRecord.createMainRecord(id, type);

		int index = startIndex + 1;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(line.startsWith("0 "))
				// Start of a new record
				break;

			final FLEFRecord child = parseChildLine(line);
			record.addChild(child);
			index ++;
		}

		record.setLineCount(index - startIndex);
		return record;
	}

	/**
	 * Parser a child line (level > 0).
	 * Example: "2 INDIVIDUAL_NAME Mario"
	 */
	private static FLEFRecord parseChildLine(final String line){
		final String[] parts = line.split(" ", 3);
		final int level = Integer.parseInt(parts[0]);
		final String tag = parts[1];
		final String value = (parts.length > 2)? parts[2]: null;

		final FLEFRecord child = FLEFRecord.createChildWithValue(level, tag, value);

		// We don't handle children of children recursively for simplicity.
		// In a real implementation, we would parse recursively.
		// For now, let's assume children are only the next level up.

		return child;
	}

	/**
	 * Serializes a record in FLEF format.
	 */
	private static String serializeRecord(final FLEFRecord record, final int level){
		final StringBuilder sb = new StringBuilder();

		// The main line of the record
		if(record.getLevel() == 0){
			if(record.getId() != null)
				sb.append("0 @")
					.append(record.getId())
					.append("@ ")
					.append(record.getType())
					.append("\n");
			else
				sb.append("0 ")
					.append(record.getType())
					.append("\n");
		}
		else{
			final String line = record.getLevel() + " " + record.getTag() +
				(record.getValue() != null? " " + record.getValue(): "");
			sb.append(line)
				.append("\n");
		}

		// Children
		for(final FLEFRecord child : record.getChildren())
			sb.append(serializeRecord(child, child.getLevel()));

		return sb.toString();
	}

	// ==================== Helper Methods for Resource Loading ====================

	/**
	 * Finds a resource in the classpath and returns its file system path.
	 *
	 * @param resourceName the resource name (e.g., "/grammar.gedg")
	 * @return the file system path as a String
	 * @throws IOException if the resource cannot be found or accessed
	 */
	private static Path getResourcePath(final String resourceName) throws IOException{
		final URL resource = FLEFFile.class.getResource(resourceName);
		if(resource == null)
			throw new IOException("Resource not found: " + resourceName);

		try{
			return Paths.get(resource.toURI());
		}
		catch(final URISyntaxException e){
			throw new IOException("Invalid resource URI: " + resourceName, e);
		}
	}

	/**
	 * Finds a grammar resource in the classpath, trying both .gedg and .gedg.txt extensions.
	 *
	 * @param baseName the base name without extension (e.g., "/gedcom_5.5.1")
	 * @return the file system path as a String
	 * @throws IOException if the resource cannot be found
	 */
	private static Path findGrammarResource(final String baseName) throws IOException{
		// Try with .gedg extension first
		final String[] extensions = {".gedg", ".gedg.txt"};
		for(final String ext : extensions){
			try{
				return getResourcePath(baseName + ext);
			}
			catch(final IOException ignored){}
		}

		throw new IOException("Grammar resource not found: " + baseName + " with .gedg or .gedg.txt extension");
	}

	/**
	 * Finds a data resource in the classpath.
	 *
	 * @param baseName  the base name without extension (e.g., "/test")
	 * @param extension the extension (e.g., ".ged", ".flef")
	 * @return the file system path as a String
	 * @throws IOException if the resource cannot be found
	 */
	private static Path findDataResource(final String baseName, final String extension) throws IOException{
		return getResourcePath(baseName + extension);
	}


	/**
	 * Example usage: loads a data file and validates it against a grammar file.
	 * Both files are loaded from the classpath.
	 */
	public static void main(String[] args){
		try{
			// Find the grammar file in the classpath (try both extensions)
			Path grammarPath = findGrammarResource("/gedcom_5.5.1");
			System.out.println("Grammar: " + grammarPath);
			// Find the data file in the classpath
			Path dataPath = findDataResource("/test", ".ged");
			System.out.println("Data: " + dataPath);

			// Load and validate in one call
			FLEFModel model = loadAndValidate(dataPath, grammarPath);

			System.out.println("✅ Valid file! " + model.getRecordCount() + " record(s) loaded.");
			System.out.println("Record types: " + model.getRecordTypes());
		}
		catch(ValidationException e){
			System.err.println("❌ Errori di validazione (" + e.getErrors().size() + "):");
			for(ValidationError err : e.getErrors()){
				System.err.println("  - " + err);
			}
		}
		catch(IOException e){
			System.err.println("❌ Errore di I/O: " + e.getMessage());
			e.printStackTrace();
		}
		catch(Exception e){
			System.err.println("❌ Errore inaspettato: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Test method that creates a sample file and reads it back.
	 */
	public static void main2(final String[] args){
		try{
			final String testFilePath = "test.flef";
			final FLEFModel model = new FLEFModel();
			final FLEFRecord header = createTestHeader();
			model.setHeader(header);
			final FLEFRecord individual = createTestIndividual();
			model.addRecord(individual);
			final FLEFRecord place = createTestPlace();
			model.addRecord(place);
			save(model, testFilePath);
			System.out.println("✅ File di test creato: " + testFilePath);

			final FLEFModel loaded = load(Path.of(testFilePath));
			System.out.println("✅ File caricato: " + loaded.getRecordCount() + " record(s)");

		}
		catch(final IOException e){
			e.printStackTrace();
		}
	}

	private static FLEFRecord createTestHeader(){
		final FLEFRecord header = new FLEFRecord();
		header.setType("HEADER");

		final FLEFRecord protocol = FLEFRecord.createChildWithValue(1, "PROTOCOL", "FLEF");
		header.addChild(protocol);

		final FLEFRecord version = FLEFRecord.createChildWithValue(2, "VERSION", "0.1.0");
		protocol.addChild(version);

		final FLEFRecord source = FLEFRecord.createChildWithValue(1, "SOURCE", "MyApp");
		header.addChild(source);

		final FLEFRecord date = FLEFRecord.createChildWithValue(1, "DATE", "2026-07-10");
		header.addChild(date);

		return header;
	}

	private static FLEFRecord createTestIndividual(){
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");

		final FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		individual.addChild(name);

		final FLEFRecord givenName = FLEFRecord.createChildWithValue(2, "INDIVIDUAL_NAME", "Mario");
		name.addChild(givenName);

		final FLEFRecord familyName = FLEFRecord.createChildWithValue(2, "FAMILY_NAME", "Rossi");
		name.addChild(familyName);

		final FLEFRecord sex = FLEFRecord.createChildWithValue(1, "SEX", "MALE");
		individual.addChild(sex);

		return individual;
	}

	private static FLEFRecord createTestPlace(){
		final FLEFRecord place = FLEFRecord.createMainRecord("P1", "PLACE");

		final FLEFRecord name = FLEFRecord.createChildWithValue(1, "NAME", "Rome");
		place.addChild(name);

		final FLEFRecord address = FLEFRecord.createChildWithValue(1, "ADDRESS", "Piazza Venezia");
		place.addChild(address);

		final FLEFRecord map = new FLEFRecord();
		map.setLevel(1);
		map.setTag("MAP");
		place.addChild(map);

		final FLEFRecord latitude = FLEFRecord.createChildWithValue(2, "LATITUDE", "41.9028");
		map.addChild(latitude);

		final FLEFRecord longitude = FLEFRecord.createChildWithValue(2, "LONGITUDE", "12.4964");
		map.addChild(longitude);

		return place;
	}

}
