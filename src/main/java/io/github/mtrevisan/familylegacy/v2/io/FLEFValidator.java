package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.FileDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.*;


/**
 * Validates a {@link FLEFModel} structure and data against a {@link FLEFGrammar}.
 */
public class FLEFValidator{

	private final FLEFGrammar grammar;


	public FLEFValidator(final FLEFGrammar grammar){
		this.grammar = Objects.requireNonNull(grammar, "grammar cannot be null");
	}

	/**
	 * Validates the given {@link FLEFModel}.
	 *
	 * @param model the model to validate
	 * @return a list of validation error messages (empty if valid)
	 */
	public List<String> validate(final FLEFModel model){
		if(model == null)
			return List.of("Model is null");

		final FileDefinition fileDef = grammar.getFileDefinition();
		final List<String> errors = new ArrayList<>();

		// 1. Validate Header
		if(fileDef.headerField() != null && model.getHeader() != null){
			final TypeDefinition headerType = grammar.getType(fileDef.headerField().type().getName());
			if(headerType != null)
				headerType.validate("header", model.getHeader(), grammar, errors);
		}

		// 2. Validate Records
		if(fileDef.recordsField() != null){
			final TypeDefinition recordsType = grammar.getType(fileDef.recordsField().type().getName());
			if(recordsType != null)
				for(final FLEFRecord record : model.getRecords())
					recordsType.validate("records." + record.getTag(), record, grammar, errors);
		}

		// 3. Validate Cross-References Integrity
		validateCrossReferences(model, errors);

		return errors;
	}

	private void validateCrossReferences(final FLEFModel model, final List<String> errors){
		final Deque<FLEFRecord> stack = new ArrayDeque<>();

		// Initializes the stack with the root of the header and all parent records
		if(model.getHeader() != null)
			stack.push(model.getHeader());
		for(final FLEFRecord root : model.getRecords())
			if(root != null)
				stack.push(root);

		// DFS traversal
		while(!stack.isEmpty()){
			final FLEFRecord record = stack.pop();

			// Check the cross-reference
			if(record.isReference() && !record.isVoid()){
				final String refId = record.getReferenceId();
				if(refId != null && !model.hasRecord(refId))
					errors.add(String.format("Dangling reference '%s' found in record '%s'", refId, record.getTag()));
			}

			// Adds subnodes to the stack
			final List<FLEFRecord> children = record.getChildren();
			if(children != null && !children.isEmpty())
				for(int i = children.size() - 1; i >= 0; i --){
					final FLEFRecord child = children.get(i);
					if(child != null)
						stack.push(child);
				}
		}
	}

}
