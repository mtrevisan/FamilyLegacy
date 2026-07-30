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
package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 */
public final class FLEFGrammar{

	private static final Pattern TAG_LINE_PATTERN = Pattern.compile(
		"^(\\+?)(\\d+)?\\s+([A-Z_]+|<<[A-Z_]+>>)\\s*(<[A-Z_]+>)?\\s*(\\{[^}]*\\})?"
	);


	private final Map<String, RecordDefinition> recordDefs = new LinkedHashMap<>();
	private final Map<String, RecordDefinition> structureDefs = new LinkedHashMap<>();

	// Internal alias mapping: short code (e.g., "INDI") -> actual record key (e.g., "INDIVIDUAL_RECORD")
	private final Map<String, String> recordAliases = new HashMap<>();

	// Default mapping for common GEDCOM short codes to full record keys
	private static final Map<String, String> DEFAULT_ALIASES = new HashMap<>();
	static{
		DEFAULT_ALIASES.put("INDI", "INDIVIDUAL_RECORD");
		DEFAULT_ALIASES.put("FAM", "GROUP_RECORD");
		DEFAULT_ALIASES.put("SOUR", "SOURCE_RECORD");
		DEFAULT_ALIASES.put("OBJE", "OBJECT_RECORD");
		DEFAULT_ALIASES.put("REPO", "REPOSITORY_RECORD");
		DEFAULT_ALIASES.put("SUBM", "SUBMITTER_RECORD");
		// Add more as needed

		DEFAULT_ALIASES.put("INDIVIDUAL", "INDIVIDUAL_RECORD");
		DEFAULT_ALIASES.put("INDIVIDUAL_EVENT", "INDIVIDUAL_EVENT_RECORD");
		DEFAULT_ALIASES.put("GROUP", "GROUP_RECORD");
		DEFAULT_ALIASES.put("GROUP_EVENT", "GROUP_EVENT_RECORD");
		DEFAULT_ALIASES.put("DNA_MATCH", "DNA_MATCH_RECORD");
		DEFAULT_ALIASES.put("PLACE", "PLACE_RECORD");
		DEFAULT_ALIASES.put("NOTE", "NOTE_RECORD");
		DEFAULT_ALIASES.put("REPOSITORY", "REPOSITORY_RECORD");
		DEFAULT_ALIASES.put("CULTURAL_NORM", "CULTURAL_NORM_RECORD");
		DEFAULT_ALIASES.put("SOURCE", "SOURCE_RECORD");
		DEFAULT_ALIASES.put("HISTORIC_EVENT", "HISTORIC_EVENT_RECORD");
		DEFAULT_ALIASES.put("RESEARCH_STATUS", "RESEARCH_STATUS_RECORD");
	}


	public static FLEFGrammar createFromPath(final Path gedgPath) throws IOException{
		try(final BufferedReader reader = Files.newBufferedReader(gedgPath, StandardCharsets.UTF_8)){
			return parse(reader);
		}
	}

	public static FLEFGrammar createFromStream(final InputStream inputStream) throws IOException{
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
			return parse(reader);
		}
	}

	public static FLEFGrammar createFromResource(final String resourcePath) throws IOException{
		final InputStream is = FLEFGrammar.class.getResourceAsStream(resourcePath);
		if(is == null)
			throw new IOException("Resource not found: " + resourcePath);

		return createFromStream(is);
	}


	private FLEFGrammar(){
		// Pre-populate aliases with default mappings
		recordAliases.putAll(DEFAULT_ALIASES);
	}


	private static FLEFGrammar parse(final BufferedReader reader) throws IOException{
		// Read all lines into memory
		final List<String> lines = new ArrayList<>();
		String line;
		while((line = reader.readLine()) != null){
			final String cleaned = stripComments(line).trim();
			if(!cleaned.isEmpty())
				lines.add(cleaned);
		}

		final Map<String, RecordDefinition> tempStructDefs = new LinkedHashMap<>();
		final Map<String, RecordDefinition> tempRecordDefs = new LinkedHashMap<>();

		RecordDefinition currentDef = null;
		String currentDefName = null;
		boolean currentIsStructure = false;
		boolean inDefinition = false;

		for(final String currentLine : lines){
			// Check if this line starts a new definition (contains " := ")
			if(currentLine.contains(":=")){
				// Save the previous definition
				if(currentDef != null){
					if(currentIsStructure)
						tempStructDefs.put(currentDefName, currentDef);
					else
						tempRecordDefs.put(currentDefName, currentDef);
				}

				// Parse the definition name
				String defName = currentLine.split(":=")[0].trim();
				if(defName.startsWith("n "))
					defName = defName.substring(2).trim();
				final int braceIdx = defName.indexOf('{');
				if(braceIdx != -1)
					defName = defName.substring(0, braceIdx).trim();

				// Determine if this is a STRUCTURE definition:
				// A structure definition has the form "n <<NAME>> {1:1}" (with angle brackets on the left side)
				final boolean isStruct = currentLine.contains("<<") && currentLine.contains(">>");
				if(isStruct){
					final int start = currentLine.indexOf("<<") + 2;
					final int end = currentLine.indexOf(">>");
					if(start > 0 && end > start)
						defName = currentLine.substring(start, end).trim();
				}

				currentDef = RecordDefinition.create(defName);
				currentDefName = defName;
				currentIsStructure = isStruct;
				inDefinition = true;
				continue;
			}

			// If we are inside a definition, parse tag lines
			if(inDefinition){
				// Skip lines that are not tag definitions
				if(currentLine.startsWith("ROOT") || currentLine.startsWith("RECORD") ||
					currentLine.startsWith("END_OF_FILE") || currentLine.startsWith("[") ||
					currentLine.startsWith("|") || currentLine.startsWith("]")){
					continue;
				}

				final Matcher m = TAG_LINE_PATTERN.matcher(currentLine);
				if(m.find()){
					final String tagName = m.group(3);
					final String cardinalityStr = m.group(5);
					final Cardinality cardinality = (cardinalityStr != null
						? Cardinality.parse(cardinalityStr)
						: Cardinality.parse("{1:1}"));

					final boolean isStructRef = (tagName.startsWith("<<") && tagName.endsWith(">>"));
					if(isStructRef){
						final String structName = tagName.substring(2, tagName.length() - 2).trim();

						// If we are in a structure definition and this is the first tag,
						// it is the root tag of the structure
						if(currentIsStructure && currentDef.getRootTag() == null)
							currentDef.setRootTag(structName);

						final TagDefinition tagDef = TagDefinition.structure(structName, cardinality, structName);
						currentDef.addTag(tagDef);
					}
					else{
						// Simple tag
						final TagDefinition tagDef = TagDefinition.simple(tagName, cardinality);
						currentDef.addTag(tagDef);

						// If we are in a structure and we don't have a root tag yet,
						// this is the root tag
						if(currentIsStructure && currentDef.getRootTag() == null)
							currentDef.setRootTag(tagName);
					}
				}
			}
		}

		// Save the last definition
		if(currentDef != null){
			if(currentIsStructure)
				tempStructDefs.put(currentDefName, currentDef);
			else
				tempRecordDefs.put(currentDefName, currentDef);
		}

		// Collect all structure names referenced in record definitions
		final Set<String> referencedStructNames = new HashSet<>();
		for(final RecordDefinition def : tempRecordDefs.values())
			for(final TagDefinition tagDef : def.getTags())
				if(tagDef.isStructure())
					referencedStructNames.add(tagDef.getStructureName());

		// Move any referenced definition that is not already in tempStructDefs from tempRecordDefs
		for(final String structName : referencedStructNames){
			if(tempStructDefs.containsKey(structName))
				// Already a structure
				continue;

			final RecordDefinition def = tempRecordDefs.remove(structName);
			if(def != null){
				// This definition is actually a structure, not a record.
				// We need to set its root tag: the first tag in its definition.
				String rootTag = null;
				for(final TagDefinition td : def.getTags())
					if(!td.isStructure()){
						rootTag = td.getName();
						break;
					}
				if(rootTag == null)
					// Fallback: use the structure name
					rootTag = structName;
				def.setRootTag(rootTag);
				tempStructDefs.put(structName, def);
			}
		}


		final FLEFGrammar grammar = new FLEFGrammar();

		for(final Map.Entry<String, RecordDefinition> entry : tempRecordDefs.entrySet()){
			final String recKey = entry.getKey();
			final RecordDefinition recDef = entry.getValue();
			final RecordDefinition resolvedDef = RecordDefinition.create(recKey);

			for(final TagDefinition tagDef : recDef.getTags()){
				if(tagDef.isStructure()){
					final String structName = tagDef.getStructureName();
					final RecordDefinition structDef = tempStructDefs.get(structName);
					if(structDef == null)
						throw new IllegalArgumentException(
							"Structure definition not found: " + structName + " (referenced in " + recKey + ")"
						);

					String rootTag = structDef.getRootTag();
					if(rootTag == null){
						rootTag = structName;
						structDef.setRootTag(rootTag);
					}
					final TagDefinition resolvedTag = TagDefinition.structure(
						rootTag, tagDef.getCardinality(), structName
					);
					resolvedDef.addTag(resolvedTag);
				}
				else
					resolvedDef.addTag(tagDef);
			}

			grammar.recordDefs.put(recKey, resolvedDef);
		}

		// Copy structure definitions
		grammar.structureDefs.putAll(tempStructDefs);

		// Ensure all structures referenced in records have root tags
		for(final RecordDefinition def : grammar.recordDefs.values())
			for(final TagDefinition tagDef : def.getTags())
				if(tagDef.isStructure()){
					final String structName = tagDef.getStructureName();
					final RecordDefinition structDef = grammar.structureDefs.get(structName);
					if(structDef == null)
						throw new IllegalArgumentException(
							"Structure definition not found: " + structName + " (referenced in " + def.getName() + ")"
						);

					if(structDef.getRootTag() == null)
						structDef.setRootTag(structName);
				}


		return grammar;
	}

	private static String stripComments(String line){
		final int commentStart = line.indexOf("/*");
		if(commentStart != -1){
			final int commentEnd = line.indexOf("*/", commentStart + 2);
			if(commentEnd != -1)
				line = line.substring(0, commentStart) + line.substring(commentEnd + 2);
			else
				line = line.substring(0, commentStart);
		}
		final int idx = line.indexOf("//");
		if(idx != -1)
			line = line.substring(0, idx);
		return line;
	}


	/**
	 * Returns an unmodifiable view of the record definitions.
	 *
	 * @return	Unmodifiable map of record definitions
	 */
	public Map<String, RecordDefinition> getRecordDefinitions(){
		return Collections.unmodifiableMap(recordDefs);
	}

	/**
	 * Returns an unmodifiable view of the structure definitions.
	 *
	 * @return	Unmodifiable map of structure definitions
	 */
	public Map<String, RecordDefinition> getStructureDefinitions(){
		return Collections.unmodifiableMap(structureDefs);
	}

	/**
	 * Looks up a record definition, first checking aliases,
	 * then using the key as-is.
	 *
	 * @param key	The code to look up (short or long)
	 * @return	The found definition, or null if none exists
	 */
	public RecordDefinition getRecordDefinition(final String key){
		// 1. Check if it's an alias
		String actualKey = recordAliases.get(key);
		if(actualKey == null)
			// 2. If not, use the original key
			actualKey = key;
		// 3. Look up in the main map
		return recordDefs.get(actualKey);
	}

	/**
	 * Looks up a structure definition by its name.
	 *
	 * @param name	The structure name (e.g., "ADDRESS_STRUCTURE")
	 * @return	The found definition, or null if none exists
	 */
	public RecordDefinition getStructureDefinition(final String name){
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
