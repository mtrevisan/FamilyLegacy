package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Validates a FLEF data model against a grammar.
 */
public final class FLEFValidator{

	private final FLEFGrammar grammar;

	public FLEFValidator(FLEFGrammar grammar){
		this.grammar = grammar;
	}

	/**
	 * Validates the entire model.
	 *
	 * @param model the data model to validate
	 * @return a list of validation errors; empty if the model is valid
	 */
	public List<ValidationError> validate(FLEFModel model){
		List<ValidationError> errors = new ArrayList<>();

		// Optionally validate header? For simplicity we'll skip header validation for now.
		// But we could add a method validateRecord for HEADER if needed.

		for(FLEFRecord record : model.getRecords()){
			String type = record.getType();
			RecordDefinition def = grammar.getRecordDefinition(type);
			if(def == null){
				errors.add(new ValidationError(record.getId(), "Unknown record type: " + type));
				continue;
			}
			validateRecord(record, def, errors, record.getId());
		}

		return errors;
	}

	private void validateRecord(FLEFRecord record, RecordDefinition def, List<ValidationError> errors, String path){
		// Count occurrences of each child tag
		Map<String, Integer> tagCounts = new HashMap<>();
		for(FLEFRecord child : record.getChildren()){
			String tag = child.getTag();
			if(tag != null){
				tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
			}
		}

		// Check each defined tag
		for(TagDefinition tagDef : def.getTags()){
			String tagName = tagDef.getName();
			int count = tagCounts.getOrDefault(tagName, 0);
			if(!tagDef.getCardinality().isValidCount(count)){
				errors.add(new ValidationError(
					path + "/" + tagName,
					"Tag '" + tagName + "' appears " + count + " times, expected " + tagDef.getCardinality()
				));
			}

			// If it's a structure, validate its children
			if(tagDef.isStructure()){
				String structName = tagDef.getStructureName();
				RecordDefinition structDef = grammar.getStructureDefinition(structName);
				if(structDef == null){
					errors.add(new ValidationError(
						path + "/" + tagName,
						"Structure definition not found: " + structName
					));
					continue;
				}
				// Find all child records with this tag
				for(FLEFRecord child : record.getChildren()){
					if(tagName.equals(child.getTag())){
						// Validate the structure inside this child
						// The child record itself represents the structure; its children are the structure's fields.
						// However, the child may have a value? For simplicity, we treat it as a container.
						// We'll validate its children against the structure definition.
						validateRecord(child, structDef, errors, path + "/" + tagName);
					}
				}
			}
		}

		// Check for unexpected tags (tags not defined in the grammar)
		Set<String> allowedTags = def.getTags().stream()
			.map(TagDefinition::getName)
			.collect(Collectors.toSet());
		for(String tag : tagCounts.keySet()){
			if(!allowedTags.contains(tag)){
				errors.add(new ValidationError(
					path,
					"Unexpected tag '" + tag + "' (not allowed in " + def.getName() + ")"
				));
			}
		}
	}

}
