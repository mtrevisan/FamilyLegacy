package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser and validator for .gedg grammar files (FLEF/GEDCOM protocol definitions).
 * <p>
 * This class performs a two-pass parsing:
 * <ol>
 *   <li>First pass: collects all record and structure definitions from the .gedg file.</li>
 *   <li>Promotion: moves any definition that is referenced as a structure from records to structures.</li>
 *   <li>Second pass: resolves structure references by replacing them with their root tag names.</li>
 * </ol>
 * <p>
 * The parser is completely generic and works with any protocol version (FLEF 0.0.9, GEDCOM 5.5.1, etc.)
 * as long as the .gedg file follows the same syntax conventions.
 */
public final class FLEFGrammar{

	private static final Pattern TAG_LINE_PATTERN = Pattern.compile(
		"^(\\+?)(\\d+)?\\s+([A-Z_]+|<<[A-Z_]+>>)\\s*(<[A-Z_]+>)?\\s*(\\{[^}]*\\})?"
	);
	private static final Pattern RECORD_DEF_LINE_PATTERN = Pattern.compile(
		"^n\\s+@<XREF:([^>]+)>@\\s+([A-Z_]+)\\s*(\\{[^}]*\\})?"
	);
	private static final Pattern SIMPLE_RECORD_DEF_LINE_PATTERN = Pattern.compile(
		"^n\\s+([A-Z_]+)\\s*(\\{[^}]*\\})?"
	);

	private final Map<String, RecordDefinition> recordDefs = new LinkedHashMap<>();
	private final Map<String, RecordDefinition> structureDefs = new LinkedHashMap<>();

	private FLEFGrammar(){
	}

	/**
	 * Loads a grammar from a .gedg file on the filesystem.
	 *
	 * @param gedgFilePath path to the protocol file
	 * @return a FLEFGrammar instance
	 * @throws IOException              if the file cannot be read
	 * @throws IllegalArgumentException if the file is malformed
	 */
	public static FLEFGrammar loadFromFile(String gedgFilePath) throws IOException{
		return loadFromPath(Path.of(gedgFilePath));
	}

	/**
	 * Loads a grammar from a .gedg file on the filesystem.
	 *
	 * @param gedgPath path to the protocol file
	 * @return a FLEFGrammar instance
	 * @throws IOException              if the file cannot be read
	 * @throws IllegalArgumentException if the file is malformed
	 */
	public static FLEFGrammar loadFromPath(Path gedgPath) throws IOException{
		try(BufferedReader reader = Files.newBufferedReader(gedgPath, StandardCharsets.UTF_8)){
			return parse(reader);
		}
	}

	/**
	 * Loads a grammar from an InputStream (e.g., from a resource in the classpath).
	 *
	 * @param inputStream the input stream to read from
	 * @return a FLEFGrammar instance
	 * @throws IOException              if the stream cannot be read
	 * @throws IllegalArgumentException if the grammar is malformed
	 */
	public static FLEFGrammar loadFromStream(InputStream inputStream) throws IOException{
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
			return parse(reader);
		}
	}

	/**
	 * Loads a grammar from a classpath resource.
	 *
	 * @param resourcePath the resource path (e.g., "/flef_0.0.9.gedg.txt")
	 * @return a FLEFGrammar instance
	 * @throws IOException              if the resource cannot be found or read
	 * @throws IllegalArgumentException if the grammar is malformed
	 */
	public static FLEFGrammar loadFromResource(String resourcePath) throws IOException{
		InputStream is = FLEFGrammar.class.getResourceAsStream(resourcePath);
		if(is == null){
			throw new IOException("Resource not found: " + resourcePath);
		}
		return loadFromStream(is);
	}

	/**
	 * Parses the grammar from a BufferedReader.
	 * This is the core parsing method that does the two-pass processing.
	 *
	 * @param reader the reader to read from
	 * @return a FLEFGrammar instance
	 * @throws IOException              if the stream cannot be read
	 * @throws IllegalArgumentException if the grammar is malformed
	 */
	private static FLEFGrammar parse(BufferedReader reader) throws IOException{
		// Read all lines into memory for easier two-pass processing
		List<String> lines = new ArrayList<>();
		String line;
		while((line = reader.readLine()) != null){
			String cleaned = stripComments(line).trim();
			if(!cleaned.isEmpty()){
				lines.add(cleaned);
			}
		}

		// ===== PASS 1: Parse definitions =====
		Map<String, RecordDefinition> tempStructDefs = new LinkedHashMap<>();
		Map<String, RecordDefinition> tempRecordDefs = new LinkedHashMap<>();
		// Store the definition name to record tag mapping
		Map<String, String> recordDefNameToTag = new LinkedHashMap<>();

		RecordDefinition currentDef = null;
		String currentDefName = null;
		boolean currentIsStructure = false;
		boolean inDefinition = false;
		boolean isFirstLineOfDefinition = true;

		for(int i = 0; i < lines.size(); i++){
			String currentLine = lines.get(i);

			// Check if this line starts a new definition (contains " := ")
			if(currentLine.contains(":=")){
				// Save the previous definition
				if(currentDef != null){
					String key;
					if(currentIsStructure){
						key = currentDefName;
						tempStructDefs.put(key, currentDef);
					}
					else{
						// For records, the key is the record tag extracted from the first line
						key = recordDefNameToTag.get(currentDefName);
						if(key == null){
							key = currentDefName; // fallback
						}
						tempRecordDefs.put(key, currentDef);
					}
				}

				// Parse the definition name
				String defName = currentLine.split(":=")[0].trim();
				if(defName.startsWith("n ")){
					defName = defName.substring(2).trim();
				}
				// Remove trailing braces like "{1:1}" if present
				int braceIdx = defName.indexOf('{');
				if(braceIdx != -1){
					defName = defName.substring(0, braceIdx).trim();
				}

				// Determine if this is a STRUCTURE definition:
				// A structure definition has the form "n <<NAME>> {1:1}" (with angle brackets on the left side)
				// A record definition has the form "n @<XREF:...>@ NAME {1:1}" or "n NAME {1:1}"
				boolean isStruct = currentLine.contains("<<") && currentLine.contains(">>");
				if(isStruct){
					// Extract the actual structure name from between << and >>
					int start = currentLine.indexOf("<<") + 2;
					int end = currentLine.indexOf(">>");
					if(start > 0 && end > start){
						defName = currentLine.substring(start, end).trim();
					}
				}

				currentDef = new RecordDefinition(defName);
				currentDefName = defName;
				currentIsStructure = isStruct;
				inDefinition = true;
				isFirstLineOfDefinition = true;
				continue;
			}

			// If we are inside a definition, parse tag lines
			if(inDefinition && currentDef != null){
				// Skip lines that are not tag definitions (e.g., "ROOT :=", "RECORD :=", "END_OF_FILE :=", "[" etc.)
				if(currentLine.startsWith("ROOT") || currentLine.startsWith("RECORD") ||
					currentLine.startsWith("END_OF_FILE") || currentLine.startsWith("[") ||
					currentLine.startsWith("|") || currentLine.startsWith("]")){
					continue;
				}

				// If this is the first line of the definition and it's a record (not a structure),
				// extract the record tag (e.g., "INDI", "FAM", "INDIVIDUAL", etc.)
				if(isFirstLineOfDefinition && !currentIsStructure){
					// Try to match the record definition line
					Matcher m1 = RECORD_DEF_LINE_PATTERN.matcher(currentLine);
					if(m1.find()){
						String recordTag = m1.group(2);
						recordDefNameToTag.put(currentDefName, recordTag);
					}
					else{
						Matcher m2 = SIMPLE_RECORD_DEF_LINE_PATTERN.matcher(currentLine);
						if(m2.find()){
							String recordTag = m2.group(1);
							recordDefNameToTag.put(currentDefName, recordTag);
						}
					}
					isFirstLineOfDefinition = false;
				}

				Matcher m = TAG_LINE_PATTERN.matcher(currentLine);
				if(m.find()){
					String tagName = m.group(3);
					String cardinalityStr = m.group(5);
					Cardinality cardinality = (cardinalityStr != null)?
						Cardinality.parse(cardinalityStr): Cardinality.parse("{1:1}");

					boolean isStructRef = tagName.startsWith("<<") && tagName.endsWith(">>");
					if(isStructRef){
						String structName = tagName.substring(2, tagName.length() - 2).trim();

						// If we are in a structure definition and this is the first tag,
						// it is the root tag of the structure
						if(currentIsStructure && currentDef.getRootTag() == null){
							currentDef.setRootTag(structName);
						}

						// Add a structure reference tag (will be resolved in PASS 2)
						TagDefinition tagDef = TagDefinition.structure(structName, cardinality, structName);
						currentDef.addTag(tagDef);
					}
					else{
						// Simple tag
						TagDefinition tagDef = TagDefinition.simple(tagName, cardinality);
						currentDef.addTag(tagDef);

						// If we are in a structure and we don't have a root tag yet,
						// this is the root tag
						if(currentIsStructure && currentDef.getRootTag() == null){
							currentDef.setRootTag(tagName);
						}
					}
				}
			}
		}

		// Save the last definition
		if(currentDef != null){
			String key;
			if(currentIsStructure){
				key = currentDefName;
				tempStructDefs.put(key, currentDef);
			}
			else{
				key = recordDefNameToTag.get(currentDefName);
				if(key == null){
					key = currentDefName;
				}
				tempRecordDefs.put(key, currentDef);
			}
		}

		// ===== PROMOTION: Move referenced structures from tempRecordDefs to tempStructDefs =====
		// Collect all structure names that are referenced in record definitions
		Set<String> referencedStructNames = new HashSet<>();
		for(RecordDefinition def : tempRecordDefs.values()){
			for(TagDefinition tagDef : def.getTags()){
				if(tagDef.isStructure()){
					referencedStructNames.add(tagDef.getStructureName());
				}
			}
		}

		// Move any referenced definition that is not already in tempStructDefs from tempRecordDefs
		for(String structName : referencedStructNames){
			if(tempStructDefs.containsKey(structName)){
				continue; // Already a structure
			}
			RecordDefinition def = tempRecordDefs.remove(structName);
			if(def != null){
				// This definition is actually a structure, not a record.
				// We need to set its root tag: the first tag in its definition.
				String rootTag = null;
				for(TagDefinition td : def.getTags()){
					if(!td.isStructure()){
						rootTag = td.getName();
						break;
					}
				}
				if(rootTag == null){
					rootTag = structName;
				}
				def.setRootTag(rootTag);
				tempStructDefs.put(structName, def);
			}
		}

		// ===== PASS 2: Resolve structure references =====
		// For each record definition, replace structure references with the actual root tag
		FLEFGrammar grammar = new FLEFGrammar();

		for(Map.Entry<String, RecordDefinition> entry : tempRecordDefs.entrySet()){
			String recKey = entry.getKey(); // This is the record tag (e.g., "INDI", "INDIVIDUAL")
			RecordDefinition recDef = entry.getValue();
			RecordDefinition resolvedDef = new RecordDefinition(recKey);

			for(TagDefinition tagDef : recDef.getTags()){
				if(tagDef.isStructure()){
					String structName = tagDef.getStructureName();
					RecordDefinition structDef = tempStructDefs.get(structName);
					if(structDef == null){
						throw new IllegalArgumentException(
							"Structure definition not found: " + structName + " (referenced in " + recKey + ")"
						);
					}
					String rootTag = structDef.getRootTag();
					if(rootTag == null){
						rootTag = structName;
						structDef.setRootTag(rootTag);
					}
					TagDefinition resolvedTag = TagDefinition.structure(
						rootTag, tagDef.getCardinality(), structName
					);
					resolvedDef.addTag(resolvedTag);
				}
				else{
					resolvedDef.addTag(tagDef);
				}
			}

			grammar.recordDefs.put(recKey, resolvedDef);
		}

		// Copy structure definitions
		grammar.structureDefs.putAll(tempStructDefs);

		// Ensure all structures referenced in records have root tags
		for(RecordDefinition def : grammar.recordDefs.values()){
			for(TagDefinition tagDef : def.getTags()){
				if(tagDef.isStructure()){
					String structName = tagDef.getStructureName();
					RecordDefinition structDef = grammar.structureDefs.get(structName);
					if(structDef == null){
						throw new IllegalArgumentException(
							"Structure definition not found: " + structName + " (referenced in " + def.getName() + ")"
						);
					}
					if(structDef.getRootTag() == null){
						structDef.setRootTag(structName);
					}
				}
			}
		}

		return grammar;
	}

	/**
	 * Removes comments from a line.
	 * Handles both /* ... * / and // style comments.
	 *
	 * @param line the line to clean
	 * @return the line without comments
	 */
	private static String stripComments(String line){
		int commentStart = line.indexOf("/*");
		if(commentStart != -1){
			int commentEnd = line.indexOf("*/", commentStart + 2);
			if(commentEnd != -1){
				line = line.substring(0, commentStart) + line.substring(commentEnd + 2);
			}
			else{
				line = line.substring(0, commentStart);
			}
		}
		int idx = line.indexOf("//");
		if(idx != -1){
			line = line.substring(0, idx);
		}
		return line;
	}

	// ===== Getters =====

	public Map<String, RecordDefinition> getRecordDefinitions(){
		return recordDefs;
	}

	public Map<String, RecordDefinition> getStructureDefinitions(){
		return structureDefs;
	}

	public RecordDefinition getRecordDefinition(String name){
		return recordDefs.get(name);
	}

	public RecordDefinition getStructureDefinition(String name){
		return structureDefs.get(name);
	}

	@Override
	public String toString(){
		return "FLEFGrammar{" +
			"records=" + recordDefs.keySet() +
			", structures=" + structureDefs.keySet() +
			'}';
	}

}
