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
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Manages reading and writing of FLAG (.flef) data files.
 * <p>
 * This class provides both plain loading (without validation) and
 * integrated loading with validation against a grammar file.
 * <p>
 * In addition, it offers static path‑based navigation methods that let you
 * access or modify any node inside a record using a dot‑separated syntax
 * with optional zero‑based indices (e.g. {@code "NAME[1].VALUE"}).
 */
public final class FLEFFile{

	// Pattern for parsing a line: "level TAG value"
	private static final Pattern LINE_PATTERN = Pattern.compile(
		"^(\\d+)\\s+([A-Z_]+)(?:\\s+(.*))?$"
	);

	// Pattern for main record line: "0 @ID@ TYPE"
	private static final Pattern RECORD_LINE_PATTERN = Pattern.compile(
		"^0\\s+@([^@]+)@\\s+([A-Z_]+)$"
	);


	private FLEFFile(){}


	// ------------------------------------------------
	// LOADING & VALIDATION
	// ------------------------------------------------

	/**
	 * Loads a FLEF data file and validates it against a grammar file.
	 *
	 * @param flefPath	Path to the {@code .flef} data file.
	 * @param gedgPath	Path to the {@code .gedg} protocol file.
	 * @return	Validated FLEFModel.
	 * @throws IOException	If files cannot be read.
	 * @throws ValidationException	If validation fails.
	 */
	public static FLEFModel loadAndValidate(final Path flefPath, final Path gedgPath)
			throws IOException, ValidationException{
		final FLEFGrammar grammar = FLEFGrammar.createFromPath(gedgPath);
		return loadWithGrammar(flefPath, grammar);
	}

	/**
	 * Loads a FLEF data file and validates it against a pre‑loaded grammar.
	 *
	 * @param flefFilePath path to the .flef data file
	 * @param grammar      the grammar to validate against
	 * @return validated FLEFModel
	 * @throws IOException         if the file cannot be read
	 * @throws ValidationException if validation fails
	 */
	public static FLEFModel loadWithGrammar(final String flefFilePath, final FLEFGrammar grammar)
			throws IOException, ValidationException{
		return loadWithGrammar(Path.of(flefFilePath), grammar);
	}

	/**
	 * Loads a FLEF data file and validates it against a pre‑loaded grammar.
	 *
	 * @param flefPath path to the .flef data file
	 * @param grammar  the grammar to validate against
	 * @return validated FLEFModel
	 * @throws IOException         if the file cannot be read
	 * @throws ValidationException if validation fails
	 */
	public static FLEFModel loadWithGrammar(final Path flefPath, final FLEFGrammar grammar)
			throws IOException, ValidationException{
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
				index = header.getLineCount() + index;
			}
			else if(isRecordLine(line)){
				final FLEFRecord record = parseRecord(lines, index);
				model.addRecord(record);
				index = record.getLineCount() + index;
			}
			else if(isEndOfFileLine(line))
				// EOF marker – stop reading
				break;
			else
				index++;
		}

		return model;
	}

	// ------------------------------------------------
	// SAVING
	// ------------------------------------------------

	/**
	 * Saves a FLEF model to a file.
	 *
	 * @param model    the model to save
	 * @param filePath the file path
	 * @throws IOException if the file cannot be written
	 */
	public static void save(final FLEFModel model, final String filePath) throws IOException{
		final StringBuilder sb = convertToString(model);
		try(final BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))){
			writer.write(sb.toString());
		}
	}

	/**
	 * Prints a FLEF model to standard output.
	 *
	 * @param model the model to print
	 */
	public static void print(final FLEFModel model){
		final StringBuilder sb = convertToString(model);
		System.out.println(sb);
	}

	private static StringBuilder convertToString(final FLEFModel model){
		final StringBuilder sb = new StringBuilder();

		// Header
		if(model.getHeader() != null)
			serializeRecord(model.getHeader(), 0, sb);

		// Records
		for(final FLEFRecord record : model.getRecords())
			serializeRecord(record, 0, sb);

		// EOF
		sb.append("0 EOF\n");
		return sb;
	}

	/**
	 * Serializes a record with proper indentation levels.
	 * The level is calculated from the depth of the tree.
	 *
	 * @param record the record to serialize
	 * @param level  the current depth (0 for root)
	 * @param sb     the string builder
	 */
	private static void serializeRecord(final FLEFRecord record, final int level, final StringBuilder sb){
		if(level == 0){
			// Main record line: "0 @ID@ TYPE"
			if(record.getId() != null){
				sb.append("0 ")
					.append(FLEFRecordUtils.formatXRef(record.getId()))
					.append(" ")
					.append(record.getTag())
					.append("\n");
			}
			else{
				// Header or other root-level record without ID
				sb.append("0 ")
					.append(record.getTag())
					.append("\n");
			}
		}
		else{
			// Child line: "level TAG value"
			sb.append(level)
				.append(" ")
				.append(record.getTag());
			if(record.getValue() != null){
				sb.append(" ")
					.append(record.getValue());
			}
			sb.append("\n");
		}

		// Recursively serialize children with incremented level
		for(final FLEFRecord child : record.getChildren()){
			serializeRecord(child, level + 1, sb);
		}
	}

	// ------------------------------------------------
	// LOW‑LEVEL PARSING & SERIALIZATION
	// ------------------------------------------------

	private static List<String> readLines(final Path filePath) throws IOException{
		final List<String> lines = new ArrayList<>();
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))){
			String line;
			while((line = reader.readLine()) != null){
				final String trimmed = line.trim();
				if(!trimmed.isEmpty())
					lines.add(trimmed);
			}
		}
		return lines;
	}

	private static boolean isHeaderLine(final String line){
		return line.equals("0 HEADER");
	}

	private static boolean isRecordLine(final String line){
		return RECORD_LINE_PATTERN.matcher(line).matches();
	}

	private static boolean isEndOfFileLine(final String line){
		return line.equals("0 EOF");
	}

	/**
	 * Parses the HEADER record.
	 * The header is a special record with no ID, just "0 HEADER" followed by children.
	 */
	private static FLEFRecord parseHeader(final List<String> lines, final int startIndex){
		final FLEFRecord header = new FLEFRecord();
		header.setTag("HEADER");

		// Start parsing from the next line with level 1
		final Stack<FLEFRecord> stack = new Stack<>();
		stack.push(header);

		int index = startIndex + 1;

		while(index < lines.size()){
			final String line = lines.get(index);

			// Stop at next root-level record or EOF
			if(isRecordLine(line) || isHeaderLine(line) || isEndOfFileLine(line))
				break;

			final FLEFRecord child = parseLine(line);
			final int level = extractLevel(line);

			// Navigate to the correct parent based on level
			while(stack.size() > level)
				stack.pop();

			// Ensure the stack has enough levels
			while(stack.size() < level)
				stack.push(stack.peek());

			stack.peek().addChild(child);
			stack.push(child);

			index++;
		}

		header.setLineCount(index - startIndex);
		return header;
	}

	/**
	 * Parses a main record (e.g., INDIVIDUAL, PLACE, etc.).
	 */
	private static FLEFRecord parseRecord(final List<String> lines, final int startIndex){
		final String firstLine = lines.get(startIndex);
		final Matcher matcher = RECORD_LINE_PATTERN.matcher(firstLine);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid record line: " + firstLine);

		final String id = matcher.group(1);
		final String type = matcher.group(2);

		final FLEFRecord record = FLEFRecord.createMainRecord(id, type);

		// Parse children using a stack to maintain hierarchy
		final Stack<FLEFRecord> stack = new Stack<>();
		stack.push(record);

		int index = startIndex + 1;
		while(index < lines.size()){
			final String line = lines.get(index);

			// Stop at next root-level record or EOF
			if(isRecordLine(line) || isHeaderLine(line) || isEndOfFileLine(line))
				break;

			final int level = extractLevel(line);
			final FLEFRecord child = parseLine(line);

			// Navigate to the correct parent based on level
			while(stack.size() > level)
				stack.pop();

			// Ensure we have enough levels in the stack
			while(stack.size() < level)
				// In case of malformed data, duplicate the current parent
				stack.push(stack.peek());

			stack.peek().addChild(child);
			stack.push(child);

			index ++;
		}

		record.setLineCount(index - startIndex);
		return record;
	}

	/**
	 * Parses a single line into a FLEFRecord (without parsing children).
	 */
	private static FLEFRecord parseLine(final String line){
		final Matcher matcher = LINE_PATTERN.matcher(line);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid line: " + line);

		final String tag = matcher.group(2);
		final String value = matcher.group(3);

		return FLEFRecord.createChildWithValue(tag, value);
	}

	/**
	 * Extracts the level from a line.
	 */
	private static int extractLevel(final String line){
		final int spaceIdx = line.indexOf(' ');
		if(spaceIdx == -1)
			throw new IllegalArgumentException("Invalid line: " + line);

		try{
			return Integer.parseInt(line.substring(0, spaceIdx));
		}
		catch(final NumberFormatException e){
			throw new IllegalArgumentException("Invalid level in line: " + line, e);
		}
	}

	// ------------------------------------------------
	// PATH‑BASED NAVIGATION (static utilities)
	// ------------------------------------------------

	/**
	 * Represents a parsed path segment (tag + optional zero‑based index).
	 */
	private static final class PathSegment{
		final String tag;
		final int index; // -1 means "first occurrence" (index 0)

		PathSegment(final String tag, final int index){
			this.tag = tag;
			this.index = index;
		}
	}

	/**
	 * Parses a dot‑separated path with optional indices into a list of segments.
	 *
	 * @param path the path string (e.g. "NAME[1].VALUE")
	 * @return a list of path segments
	 * @throws IllegalArgumentException if the path is malformed
	 */
	private static List<PathSegment> parsePath(final String path){
		final List<PathSegment> segments = new ArrayList<>();
		for(String part : path.split("\\.")){
			int idx = -1;
			final int bracketStart = part.indexOf('[');
			if(bracketStart != -1){
				final int bracketEnd = part.indexOf(']', bracketStart);
				if(bracketEnd == -1)
					throw new IllegalArgumentException("Missing closing ']' in path: " + path);

				final String indexStr = part.substring(bracketStart + 1, bracketEnd);
				idx = Integer.parseInt(indexStr);
				part = part.substring(0, bracketStart);
			}
			segments.add(new PathSegment(part, idx));
		}
		return segments;
	}

	/**
	 * Retrieves the {@link FLEFRecord} at the given dot‑separated path.
	 * Each segment may have an optional zero‑based index in square brackets.
	 * If no index is given, the first child with that tag is used.
	 *
	 * @param root the starting record
	 * @param path the path (e.g. "NAME[1].VALUE")
	 * @return an {@code Optional} containing the found record, or empty if not found
	 * @throws IllegalArgumentException if the path is malformed
	 */
	public static Optional<FLEFRecord> getRecordByPath(final FLEFRecord root, final String path){
		if(root == null || path == null || path.isEmpty())
			return Optional.empty();

		final List<PathSegment> segments = parsePath(path);
		FLEFRecord current = root;
		for(final PathSegment seg : segments){
			if(current == null)
				break;

			final List<FLEFRecord> children = current.findChildren(seg.tag);
			int idx = (seg.index < 0? 0: seg.index); // <- normalize -1 to 0
			if(idx >= children.size()){
				current = null;

				break;
			}
			current = children.get(idx);
		}
		return Optional.ofNullable(current);
	}

	/**
	 * Retrieves the value ({@code String}) of the node identified by the path.
	 *
	 * @param root the starting record
	 * @param path the dot‑separated path
	 * @return the value, or {@code null} if not found or the node has no value
	 */
	public static String getValueByPath(final FLEFRecord root, final String path){
		return getRecordByPath(root, path)
			 .map(FLEFRecord::getValue)
			 .orElse(null);
	}

	/**
	 * Sets the value at the given path. If necessary, missing nodes are created.
	 * For indexed segments, if the index equals the current child count, a new child is appended.
	 * If the index is greater than the count, an exception is thrown.
	 *
	 * @param root  The record to modify.
	 * @param path  The dot‑separated path with optional indices.
	 * @param value The new value.
	 * @throws IllegalArgumentException If the path is malformed or the index is out of range.
	 */
	public static void setValueByPath(final FLEFRecord root, final String path, final String value){
		if(root == null || path == null)
			return;

		final List<PathSegment> segments = parsePath(path);
		FLEFRecord current = root;
		for(int i = 0; i < segments.size(); i++){
			final PathSegment seg = segments.get(i);
			final List<FLEFRecord> children = current.findChildren(seg.tag);
			int idx = (seg.index < 0? 0: seg.index); // <- normalize -1 to 0

			if(idx < children.size())
				current = children.get(idx);
			else if(idx == children.size()){
				// append a new child
				final FLEFRecord newChild = FLEFRecord.createChild(seg.tag);
				current.addChild(newChild);
				current = newChild;
			}
			else
				throw new IllegalArgumentException(
					"Index " + seg.index + " out of range for tag " + seg.tag +
						" (size " + children.size() + ")");

			if(i == segments.size() - 1)
				current.setValue(value);
		}
	}

	// ------------------------------------------------
	// RESOURCE LOADING HELPERS
	// ------------------------------------------------

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

	private static Path findGrammarResource(final String baseName) throws IOException{
		// try .gedg first, then .gedg.txt
		final String[] extensions = {".gedg", ".gedg.txt"};
		for(final String ext : extensions){
			try{
				return getResourcePath(baseName + ext);
			}
			catch(final IOException ignored){
				// continue
			}
		}
		throw new IOException("Grammar resource not found: " + baseName + " with .gedg or .gedg.txt");
	}

	private static Path findDataResource(final String baseName, final String extension) throws IOException{
		return getResourcePath(baseName + extension);
	}


	// ------------------------------------------------
	// MAIN – EXAMPLE USAGE
	// ------------------------------------------------

	public static void main(final String[] args){
		try{
			final Path grammarPath = findGrammarResource("/gedcom_5.5.1");
			System.out.println("Grammar: " + grammarPath);

			final Path dataPath = findDataResource("/test", ".ged");
			System.out.println("Data: " + dataPath);

			final FLEFModel model = loadAndValidate(dataPath, grammarPath);
			System.out.println("✅ Valid file! " + model.getRecordCount() + " record(s) loaded.");
			System.out.println("Record types: " + model.getRecordTypes());

			// Example: access a value via path
			final FLEFRecord firstIndividual = model.getRecordsByType("INDIVIDUAL").get(0);
			final String name = FLEFFile.getValueByPath(firstIndividual, "NAME.VALUE");
			System.out.println("First individual's name: " + name);

		}
		catch(final ValidationException e){
			System.err.println("❌ Validation errors (" + e.getErrors().size() + "):");
			for(final ValidationError err : e.getErrors()){
				System.err.println("  - " + err);
			}
		}
		catch(final IOException e){
			System.err.println("❌ I/O error: " + e.getMessage());
			e.printStackTrace();
		}
		catch(final Exception e){
			System.err.println("❌ Unexpected error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Alternative test that creates a sample file and reads it back.
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
			System.out.println("✅ Test file created: " + testFilePath);

			final FLEFModel loaded = load(Path.of(testFilePath));
			System.out.println("✅ File loaded: " + loaded.getRecordCount() + " record(s)");

			// test path navigation
			final FLEFRecord loadedInd = loaded.getRecordsByType("INDIVIDUAL").get(0);
			final String name = FLEFFile.getValueByPath(loadedInd, "NAME.VALUE");
			System.out.println("Loaded individual name: " + name);

		}
		catch(final IOException e){
			e.printStackTrace();
		}
	}

	private static FLEFRecord createTestHeader(){
		final FLEFRecord header = new FLEFRecord();
		header.setTag("HEADER");

		final FLEFRecord protocol = FLEFRecord.createChildWithValue("PROTOCOL", "FLEF");
		header.addChild(protocol);

		final FLEFRecord version = FLEFRecord.createChildWithValue("VERSION", "0.1.0");
		protocol.addChild(version);

		final FLEFRecord source = FLEFRecord.createChildWithValue("SOURCE", "MyApp");
		header.addChild(source);

		final FLEFRecord date = FLEFRecord.createChildWithValue("DATE", "2026-07-10");
		header.addChild(date);

		return header;
	}

	private static FLEFRecord createTestIndividual(){
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");

		final FLEFRecord name = FLEFRecord.createChild("NAME");
		individual.addChild(name);

		final FLEFRecord givenName = FLEFRecord.createChildWithValue("VALUE", "Mario");
		name.addChild(givenName);

		final FLEFRecord sex = FLEFRecord.createChildWithValue("SEX", "MALE");
		individual.addChild(sex);

		return individual;
	}

	private static FLEFRecord createTestPlace(){
		final FLEFRecord place = FLEFRecord.createMainRecord("P1", "PLACE");

		final FLEFRecord name = FLEFRecord.createChildWithValue("NAME", "Rome");
		place.addChild(name);

		final FLEFRecord map = FLEFRecord.createChild("MAP");
		place.addChild(map);

		final FLEFRecord latitude = FLEFRecord.createChildWithValue("LATITUDE", "41.9028");
		map.addChild(latitude);

		final FLEFRecord longitude = FLEFRecord.createChildWithValue("LONGITUDE", "12.4964");
		map.addChild(longitude);

		return place;
	}

}
