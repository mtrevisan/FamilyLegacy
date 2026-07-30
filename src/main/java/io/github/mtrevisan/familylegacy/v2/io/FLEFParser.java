package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationError;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationException;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Manages reading, parsing, and writing of FLAG (.flef) data files and raw text contents.
 * <p>
 * This class provides plain loading (without validation) as well as integrated
 * loading with validation against a grammar file. It supports reading directly from
 * {@link Path}, file paths, raw {@link String} content, or an input {@link Reader}.
 * <p>
 * In addition, it offers static path‑based navigation methods that let you access
 * or modify any node inside a record using a dot‑separated syntax with optional
 * zero‑based indices (e.g. {@code "NAME[1].VALUE"}).
 */
public final class FLEFParser{

	private static final Pattern LINE_PATTERN = Pattern.compile("^(\\d+)\\s+([a-zA-Z0-9_]+)(?:\\s+(.*))?$");
	private static final Pattern RECORD_LINE_PATTERN = Pattern.compile("^0\\s+@([^@]+)@\\s+([a-zA-Z0-9_]+)$");


	private FLEFParser(){}


	public static FLEFModel loadAndValidate(final Path flefPath, final Path gedgPath)
		throws IOException, ValidationException{
		final FLEFGrammar grammar = FLEFGrammar.createFromPath(gedgPath);
		return loadWithGrammar(flefPath, grammar);
	}

	public static FLEFModel loadWithGrammar(final Path flefPath, final FLEFGrammar grammar)
		throws IOException, ValidationException{
		final FLEFModel model = load(flefPath);
		return validateModel(model, grammar);
	}

	public static FLEFModel parseWithGrammar(final String content, final FLEFGrammar grammar)
		throws ValidationException{
		final FLEFModel model = parse(content);
		return validateModel(model, grammar);
	}

	public static FLEFModel load(final Path filePath) throws IOException{
		try(final FileReader reader = new FileReader(filePath.toFile())){
			return parse(reader);
		}
	}

	public static FLEFModel parse(final String content){
		if(content == null || content.isBlank()){
			return new FLEFModel();
		}
		try(final StringReader reader = new StringReader(content)){
			return parse(reader);
		}
		catch(final IOException e){
			throw new IllegalStateException("Error parsing content", e);
		}
	}

	public static FLEFModel parse(final Reader reader) throws IOException{
		final List<String> lines = readLines(reader);
		return parseModelFromLines(lines);
	}

	private static FLEFModel validateModel(final FLEFModel model, final FLEFGrammar grammar)
		throws ValidationException{
		final FLEFValidator validator = FLEFValidator.create(grammar);
		final List<ValidationError> errors = validator.validate(model);
		if(!errors.isEmpty()){
			throw ValidationException.create(errors);
		}
		return model;
	}

	private static List<String> readLines(final Reader rawReader) throws IOException{
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = (rawReader instanceof BufferedReader)
			? (BufferedReader)rawReader
			: new BufferedReader(rawReader);

		String line;
		while((line = reader.readLine()) != null){
			final String trimmed = line.trim();
			if(!trimmed.isEmpty()){
				lines.add(trimmed);
			}
		}
		return lines;
	}

	private static FLEFModel parseModelFromLines(final List<String> lines){
		final FLEFModel model = new FLEFModel();

		int index = 0;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(line.equals("0 header") || line.equals("0 HEADER")){
				final FLEFRecord header = parseBlock(lines, index, "header");
				model.setHeader(header);
				index += header.getLineCount();
			}
			else if(RECORD_LINE_PATTERN.matcher(line).matches()){
				final FLEFRecord record = parseMainRecord(lines, index);
				model.addRecord(record);
				index += record.getLineCount();
			}
			else if(line.equals("0 EOF")){
				break;
			}
			else{
				index++;
			}
		}

		return model;
	}

	private static FLEFRecord parseMainRecord(final List<String> lines, final int startIndex){
		final String firstLine = lines.get(startIndex);
		final Matcher matcher = RECORD_LINE_PATTERN.matcher(firstLine);
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid record line: " + firstLine);
		}

		final String id = matcher.group(1);
		final String type = matcher.group(2);

		final FLEFRecord record = FLEFRecord.createMainRecord(id, type);
		final Stack<FLEFRecord> stack = new Stack<>();
		stack.push(record);

		int index = startIndex + 1;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(RECORD_LINE_PATTERN.matcher(line).matches() || line.equals("0 EOF")){
				break;
			}

			final int level = extractLevel(line);
			final FLEFRecord child = parseLine(line);

			while(stack.size() > level){
				stack.pop();
			}
			while(stack.size() < level){
				stack.push(stack.peek());
			}

			stack.peek().addChild(child);
			stack.push(child);

			index++;
		}

		record.setLineCount(index - startIndex);
		return record;
	}

	private static FLEFRecord parseBlock(final List<String> lines, final int startIndex, final String blockTag){
		final FLEFRecord root = FLEFRecord.createChild(blockTag);
		final Stack<FLEFRecord> stack = new Stack<>();
		stack.push(root);

		int index = startIndex + 1;
		while(index < lines.size()){
			final String line = lines.get(index);
			if(RECORD_LINE_PATTERN.matcher(line).matches() || line.equals("0 EOF")){
				break;
			}

			final int level = extractLevel(line);
			final FLEFRecord child = parseLine(line);

			while(stack.size() > level){
				stack.pop();
			}
			while(stack.size() < level){
				stack.push(stack.peek());
			}

			stack.peek().addChild(child);
			stack.push(child);

			index++;
		}

		root.setLineCount(index - startIndex);
		return root;
	}

	private static FLEFRecord parseLine(final String line){
		final Matcher matcher = LINE_PATTERN.matcher(line);
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid line structure: " + line);
		}

		final String tag = matcher.group(2);
		final String value = matcher.group(3);

		return FLEFRecord.createChildWithValue(tag, value);
	}

	private static int extractLevel(final String line){
		final int spaceIdx = line.indexOf(' ');
		if(spaceIdx == -1){
			throw new IllegalArgumentException("Invalid line: " + line);
		}
		return Integer.parseInt(line.substring(0, spaceIdx));
	}


}
