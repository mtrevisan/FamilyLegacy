package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.*;


public class FLEFValidator{

	private final FLEFGrammar grammar;

	public FLEFValidator(FLEFGrammar grammar){
		this.grammar = grammar;
	}

	public List<ValidationError> validate(FLEFModel model){
		List<ValidationError> errors = new ArrayList<>();

		// Validate header
		FLEFRecord header = model.getHeader();
		if(header == null){
			errors.add(new ValidationError("Missing Header"));
		}
		else{
			validateRecordAgainstType(header, "Header", errors, new HashSet<>());
		}

		// Validate records
		FLEFGrammar.TypeDefinition recordUnion = grammar.getType("Record");
		if(!(recordUnion instanceof FLEFGrammar.UnionType)){
			errors.add(new ValidationError("Record type must be a union"));
			return errors;
		}
		FLEFGrammar.UnionType union = (FLEFGrammar.UnionType)recordUnion;
		Set<String> allowedRecordTypes = union.getChoices().keySet();

		for(FLEFRecord record : model.getRecords()){
			String type = record.getTag();
			if(!allowedRecordTypes.contains(type)){
				errors.add(new ValidationError("Unknown record type: " + type));
				continue;
			}
			FLEFGrammar.TypeDefinition recordDef = grammar.getType(type);
			if(!(recordDef instanceof FLEFGrammar.RecordType)){
				errors.add(new ValidationError("Record type " + type + " is not a record"));
				continue;
			}
			validateRecordAgainstType(record, type, errors, new HashSet<>());
		}

		return errors;
	}

	private void validateRecordAgainstType(FLEFRecord record, String typeName,
		List<ValidationError> errors,
		Set<String> processedTypes){
		if(processedTypes.contains(typeName)){
			return; // avoid infinite recursion
		}
		processedTypes = new HashSet<>(processedTypes);
		processedTypes.add(typeName);

		FLEFGrammar.TypeDefinition typeDef = grammar.getType(typeName);
		if(typeDef == null){
			errors.add(new ValidationError("Type not found: " + typeName));
			return;
		}

		if(typeDef instanceof FLEFGrammar.StructType){
			validateStruct(record, (FLEFGrammar.StructType)typeDef, errors, processedTypes);
		}
		else if(typeDef instanceof FLEFGrammar.UnionType){
			// Should not happen for records
			errors.add(new ValidationError("Expected struct, got union for type " + typeName));
		}
		else if(typeDef instanceof FLEFGrammar.ReferenceType){
			// Check value is a valid reference
			if(!record.isReference() && !((FLEFGrammar.ReferenceType)typeDef).isVoidable()){
				errors.add(new ValidationError("Expected reference, got: " + record.getValue()));
			}
		}
		else if(typeDef instanceof FLEFGrammar.EnumType){
			// not applicable
		}
		else{
			// scalar
		}
	}

	private void validateStruct(FLEFRecord record, FLEFGrammar.StructType struct,
		List<ValidationError> errors,
		Set<String> processedTypes){
		// Group children by tag
		Map<String, List<FLEFRecord>> childrenByTag = new HashMap<>();
		for(FLEFRecord child : record.getChildren()){
			childrenByTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>()).add(child);
		}

		// Check each field definition
		for(FLEFGrammar.FieldDefinition field : struct.getFields()){
			String fieldName = field.getName();
			List<FLEFRecord> children = childrenByTag.getOrDefault(fieldName, Collections.emptyList());
			int count = children.size();
			boolean required = field.getCardinality() == FLEFGrammar.Cardinality.REQUIRED;
			boolean oneOrMore = field.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE;
			boolean optional = field.getCardinality() == FLEFGrammar.Cardinality.OPTIONAL ||
										 field.getCardinality() == FLEFGrammar.Cardinality.ZERO_OR_MORE;

			if(required && count == 0){
				errors.add(new ValidationError("Missing required field: " + fieldName + " in " + record.getTag()));
				continue;
			}
			if(oneOrMore && count == 0){
				errors.add(new ValidationError("Missing at least one occurrence of field: " + fieldName + " in " + record.getTag()));
				continue;
			}

			// Validate each child against the field type
			FLEFGrammar.TypeDefinition fieldType = field.getType();
			for(FLEFRecord child : children){
				// If field type is a struct, validate child as that struct
				if(fieldType instanceof FLEFGrammar.StructType){
					validateRecordAgainstType(child, fieldType.getName(), errors, processedTypes);
				}
				else if(fieldType instanceof FLEFGrammar.UnionType){
					// A union field must have a child that is a choice (one of the variants)
					// The child's tag should be one of the union choice names
					FLEFGrammar.UnionType union = (FLEFGrammar.UnionType)fieldType;
					if(!union.getChoices().containsKey(child.getTag())){
						errors.add(new ValidationError("Invalid union choice: " + child.getTag() +
																	 " for field " + fieldName + " in " + record.getTag()));
					}
					else{
						// Validate the choice's content
						FLEFGrammar.TypeDefinition choiceType = union.getChoices().get(child.getTag());
						if(choiceType instanceof FLEFGrammar.StructType){
							// The child should have children that match the struct
							validateRecordAgainstType(child, choiceType.getName(), errors, processedTypes);
						}
						else if(choiceType instanceof FLEFGrammar.ReferenceType ||
									  choiceType instanceof FLEFGrammar.ScalarType ||
									  choiceType instanceof FLEFGrammar.EnumType){
							// For scalar choice, the child should have a value
							// and no children? Actually, for scalar union choices, we might have stored the value directly.
							// We need to adapt representation.
							// In our parser, we store scalar union choices as a child with the choice tag and a value.
							// So the child's value should be set.
							if(child.getValue() == null && child.hasChildren()){
								errors.add(new ValidationError("Union choice " + child.getTag() +
																			 " expected scalar value but has children"));
							}
						}
					}
				}
				else if(fieldType instanceof FLEFGrammar.ReferenceType){
					if(!child.isReference() && !((FLEFGrammar.ReferenceType)fieldType).isVoidable()){
						errors.add(new ValidationError("Expected reference for field " + fieldName +
																	 " in " + record.getTag()));
					}
				}
				else if(fieldType instanceof FLEFGrammar.EnumType){
					// Enum value should match one of the enum values or custom if allowed
					FLEFGrammar.EnumType enumType = (FLEFGrammar.EnumType)fieldType;
					if(!enumType.getValues().contains(child.getValue()) && !enumType.isAllowCustom()){
						errors.add(new ValidationError("Invalid enum value: " + child.getValue() +
																	 " for field " + fieldName + " in " + record.getTag()));
					}
				}
				else{
					// scalar: just check that value is not empty (if required)
				}
			}
		}

		// Check for unexpected fields
		Set<String> definedFields = new HashSet<>();
		for(FLEFGrammar.FieldDefinition field : struct.getFields()){
			definedFields.add(field.getName());
		}
		for(String tag : childrenByTag.keySet()){
			if(!definedFields.contains(tag)){
				// Allow "id" field in records even if not defined? We'll allow it as a special case.
				if("id".equals(tag) && record.getId() != null){
					// It's the record's own id, not a child field.
					continue;
				}
				errors.add(new ValidationError("Unexpected field: " + tag + " in " + record.getTag()));
			}
		}
	}

}
