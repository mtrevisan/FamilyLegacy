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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Validates a FLEFModel against a FLEFGrammar.
 * <p>
 * Checks that:
 * <ul>
 *   <li>All records are defined in the grammar</li>
 *   <li>All child tags conform to the grammar's cardinality constraints</li>
 *   <li>Structure references are resolved and validated recursively</li>
 * </ul>
 */
public final class FLEFValidator{

	private final FLEFGrammar grammar;
	private final List<ValidationError> errors = new ArrayList<>();


	/**
	 * Creates a new validator with the given grammar.
	 *
	 * @param grammar	The grammar to validate against
	 */
	public static FLEFValidator create(final FLEFGrammar grammar){
		return new FLEFValidator(grammar);
	}


	private FLEFValidator(final FLEFGrammar grammar){
		this.grammar = Objects.requireNonNull(grammar, "Grammar cannot be null");
	}


	/**
	 * Validates the given model against the grammar.
	 *
	 * @param model	The model to validate
	 * @return	A list of validation errors; empty if the model is valid
	 */
	public List<ValidationError> validate(final FLEFModel model){
		errors.clear();
		if(model == null){
			errors.add(ValidationError.create("", "Model is null"));
			return errors;
		}

		// Validate each record in the model
		for(final FLEFRecord record : model.getRecords()){
			final String type = record.getType();
			final RecordDefinition def = grammar.getRecordDefinition(type);
			if(def == null){
				errors.add(ValidationError.create(
					(record.getId() != null? record.getId(): ""),
					"Unknown record type: " + type
				));
				continue;
			}
			validateRecord(record, def, (record.getId() != null? record.getId(): type));
		}

		return Collections.unmodifiableList(errors);
	}

	/**
	 * Validates a single record against its definition.
	 *
	 * @param record	The record to validate
	 * @param def	The definition of the record
	 * @param path	The current path for error reporting
	 */
	private void validateRecord(final FLEFRecord record, final RecordDefinition def, final String path){
		// Count occurrences of each child tag
		final Map<String, Integer> tagCounts = new HashMap<>();
		for(final FLEFRecord child : record.getChildren()){
			final String tag = child.getTag();
			if(tag != null)
				tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
		}

		// Check each defined tag
		for(final TagDefinition tagDef : def.getTags()){
			final String tagName = tagDef.getName();
			final int count = tagCounts.getOrDefault(tagName, 0);
			final Cardinality card = tagDef.getCardinality();

			if(!card.isValidCount(count)){
				errors.add(ValidationError.create(
					path + "/" + tagName,
					"Tag '" + tagName + "' appears " + count + " times, expected " + card
				));
			}

			// If it's a structure, validate its children recursively
			if(tagDef.isStructure()){
				final String structName = tagDef.getStructureName();
				final RecordDefinition structDef = grammar.getStructureDefinition(structName);
				if(structDef == null){
					errors.add(ValidationError.create(
						path + "/" + tagName,
						"Structure definition not found: " + structName
					));
					continue;
				}
				// Validate each child with this tag against the structure definition
				for(final FLEFRecord child : record.getChildren())
					if(tagName.equals(child.getTag()))
						validateRecord(child, structDef, path + "/" + tagName);
			}
		}

		// Check for unexpected tags
		final Set<String> allowedTags = new HashSet<>();
		for(final TagDefinition tagDef : def.getTags())
			allowedTags.add(tagDef.getName());
		for(final String tag : tagCounts.keySet())
			if(!allowedTags.contains(tag)){
				errors.add(ValidationError.create(
					path,
					"Unexpected tag '" + tag + "' (not allowed in " + def.getName() + ")"
				));
			}
	}

}
