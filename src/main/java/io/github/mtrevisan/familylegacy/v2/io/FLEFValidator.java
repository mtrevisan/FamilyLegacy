package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FileDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Validates a {@link FLEFModel} structure and data against a {@link FLEFGrammar}.
 */
public class FLEFValidator{

	private static final String DOT = ".";
	private static final String FIELD_HEADER = "header";


	private final FLEFGrammar grammar;


	public FLEFValidator(final FLEFGrammar grammar){
		this.grammar = Objects.requireNonNull(grammar, "grammar cannot be null");
	}


	/**
	 * Performs complete validation (both syntactic and semantic).
	 */
	public List<String> validateAll(final FLEFModel model){
		final List<String> errors = validateSchema(model);

		// Only run semantic resolution if the AST is syntactically sound
		if(errors.isEmpty())
			errors.addAll(validateIntegrity(model));

		return errors;
	}

	/**
	 * Syntactically validates the given {@link FLEFModel}.
	 * Validates schema structure, types, cardinalities, and enum constraints.
	 *
	 * @param model the model to validate
	 * @return a list of validation error messages (empty if valid)
	 */
	public List<String> validateSchema(final FLEFModel model){
		if(model == null)
			return List.of("Model is null");

		final FileDefinition fileDef = grammar.getFileDefinition();

		final List<String> errors = new ArrayList<>();

		// 1. Validate Header
		if(fileDef.headerField() != null && model.getHeader() != null){
			final TypeDefinition headerType = grammar.getType(fileDef.headerField().type().getName());
			if(headerType != null)
				headerType.validate(FIELD_HEADER, model.getHeader(), grammar, errors);
		}

		// 2. Validate Records
		if(fileDef.recordsField() != null){
			final TypeDefinition recordsType = grammar.getType(fileDef.recordsField().type().getName());
			if(recordsType != null)
				for(final FLEFRecord record : model.getRecords())
					recordsType.validate("records." + record.getTag(), record, grammar, errors);
		}

		return errors;
	}


	/**
	 * Semantically validates the given {@link FLEFModel}.
	 * Validates referential integrity, symbol resolution, and ID uniqueness.
	 *
	 * @param model the model to validate
	 * @return a list of validation error messages (empty if valid)
	 */
	public List<String> validateIntegrity(final FLEFModel model){
		final List<String> errors = new ArrayList<>();

		// 1. First Pass: Collect all declared record IDs and verify uniqueness
		final Set<String> declaredIds = new HashSet<>();
		collectDeclaredIds(model, declaredIds, errors);

		// 2. Second Pass: Verify cross-reference resolution against declared IDs
		verifyReferences(model, errors);

		return errors;
	}

	/**
	 * Iteratively traverses all records in the FLEFModel to collect declared IDs and detect duplicates.
	 */
	private void collectDeclaredIds(final FLEFModel model, final Set<String> declaredIds, final List<String> errors){
		record TraversalNode(FLEFRecord record, String path){}

		final Deque<TraversalNode> stack = new ArrayDeque<>();

		// Push all top-level records from the model onto the stack
		final List<FLEFRecord> topLevelRecords = model.getRecords();
		for(int i = topLevelRecords.size() - 1; i >= 0; i --){
			final FLEFRecord topRecord = topLevelRecords.get(i);
			final String path = topRecord.getTag();
			stack.push(new TraversalNode(topRecord, path));
		}

		while(!stack.isEmpty()){
			final TraversalNode current = stack.pop();
			final FLEFRecord record = current.record();
			final String path = current.path();

			// Check if this record defines an ID
			final String id = record.findRecordId();
			if(id != null && !declaredIds.add(id))
				errors.add(String.format("Duplicate record ID '%s' found at '%s'", id, path));

			// Push children onto the stack in reverse order to preserve original sequence
			final List<FLEFRecord> children = record.getChildren();
			for(int i = children.size() - 1; i >= 0; i --){
				final FLEFRecord child = children.get(i);
				final String childPath = path + DOT + child.getTag();
				stack.push(new TraversalNode(child, childPath));
			}
		}
	}

	/**
	 * Iteratively traverses all records in the FLEFModel to verify cross-reference resolution.
	 */
	private void verifyReferences(final FLEFModel model, final List<String> errors){
		record TraversalNode(FLEFRecord record, String path){}

		final Deque<TraversalNode> stack = new ArrayDeque<>();

		// Push all top-level records from the model onto the stack
		final List<FLEFRecord> topLevelRecords = model.getRecords();
		for(int i = topLevelRecords.size() - 1; i >= 0; i --){
			final FLEFRecord topRecord = topLevelRecords.get(i);
			final String path = topRecord.getTag();
			stack.push(new TraversalNode(topRecord, path));
		}

		while(!stack.isEmpty()){
			final TraversalNode current = stack.pop();
			final FLEFRecord record = current.record();
			final String path = current.path();

			// Verify reference node target resolution
			if(record.isReference()){
				if(record.isVoid()){
					if(record.getValue() != null && !record.getValue().isBlank())
						errors.add(String.format("Void reference at '%s' must not specify a target identifier", path));
				}
				else{
					final String targetId = record.getValue();
					if(XRefHelper.isReference(targetId)){
						final FLEFRecord target = model.getRecordById(targetId);
						if(target == null)
							errors.add(String.format("Unresolved cross-reference '%s' at '%s': target record does not exist",
								targetId, path));
					}
				}
			}

			// Push children onto the stack in reverse order
			final List<FLEFRecord> children = record.getChildren();
			for(int i = children.size() - 1; i >= 0; i --){
				final FLEFRecord child = children.get(i);
				final String childPath = path + DOT + child.getTag();
				stack.push(new TraversalNode(child, childPath));
			}
		}
	}


	//TODO validateBusinessRules
	//	usato se oltre ai riferimenti incrociati la validazione semantica include vincoli di dominio logico (ad esempio "la data di morte non può precedere la data di nascita").
}
