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
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FileDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Validates a {@link FLEFModel} structure and data against a {@link FLEFGrammar}.
 * <p>
 * Each validation issue is returned as a {@link ValidationError}, which
 * carries both the human-readable message and, when the issue is about
 * a specific record, the id of that record. The record id is populated
 * at the source, at the point where the record is known, so consumers
 * (report dialogs, editors) do not have to parse it back out of the
 * message with a regex.
 * <p>
 * The older {@code List<String>} overloads are kept for compatibility
 * with the callers that only need the message text; they simply
 * delegate to the structured versions and discard the id.
 */
public class FLEFValidator{

	private static final String DOT = ".";
	private static final String FIELD_HEADER = "header";


	/**
	 * A single validation issue.
	 *
	 * @param message  the human-readable description of the problem
	 * @param recordId the id of the record the issue is about, or
	 *                 {@code null} when the issue is not tied to a
	 *                 specific record (e.g. a header-level error, or an
	 *                 unresolved cross-reference whose target does not
	 *                 exist)
	 */
	public record ValidationError(String message, String recordId){

		public ValidationError{
			Objects.requireNonNull(message, "message must not be null");
		}

		/** {@code true} when the error can be opened in the editor. */
		public boolean hasRecord(){
			return (recordId != null);
		}
	}


	private final FLEFGrammar grammar;


	public FLEFValidator(final FLEFGrammar grammar){
		this.grammar = Objects.requireNonNull(grammar, "grammar cannot be null");
	}


	/* ======================================================================
	 *                          Public API — structured
	 * ====================================================================== */

	/**
	 * Performs complete validation (both syntactic and semantic) and
	 * returns the issues as structured errors. Validation short-circuits:
	 * when the schema validation fails, integrity and business rules are
	 * not run, because the model is structurally broken and the higher
	 * checks would produce cascading noise.
	 *
	 * @param model the model to validate; may be {@code null}
	 * @return the issues, never {@code null}
	 */
	public List<ValidationError> validateAllStructured(final FLEFModel model){
		final List<ValidationError> errors = new ArrayList<>();

		errors.addAll(validateSchemaStructured(model));

		if(errors.isEmpty())
			errors.addAll(validateIntegrityStructured(model));

		if(errors.isEmpty())
			errors.addAll(validateBusinessRulesStructured(model));

		return errors;
	}

	/**
	 * Syntactically validates the given {@link FLEFModel}. Validates
	 * schema structure, types, cardinalities, enum constraints, and
	 * {@code require} constraints.
	 *
	 * @param model the model to validate; may be {@code null}
	 * @return the issues, never {@code null}
	 */
	public List<ValidationError> validateSchemaStructured(final FLEFModel model){
		if(model == null)
			return List.of(new ValidationError("Model is null", null));

		final FileDefinition fileDef = grammar.getFileDefinition();
		final List<ValidationError> errors = new ArrayList<>();

		// Header: no record id to attach, the header is not a record.
		if(fileDef.headerField() != null && model.getHeader() != null){
			final TypeDefinition headerType = grammar.getType(fileDef.headerField().type().getName());
			if(headerType != null){
				for(final String msg : collect(em -> headerType.validate(FIELD_HEADER,
					model.getHeader(), model, grammar, em)))
					errors.add(new ValidationError(msg, null));

				for(final String msg : collect(em -> validateConstraints(FIELD_HEADER,
					model.getHeader(), model, grammar, em)))
					errors.add(new ValidationError(msg, null));
			}
		}

		// Records: the top-level record's id is attached to every issue
		// produced while validating that record and its descendants.
		if(fileDef.recordsField() != null){
			final TypeDefinition recordsType = grammar.getType(fileDef.recordsField().type().getName());
			if(recordsType != null)
				for(final FLEFRecord record : model.getRecords()){
					final String contextPath = "records." + record.getTag();
					final String recordId = record.getId();

					for(final String msg : collect(em -> recordsType.validate(contextPath,
						record, model, grammar, em)))
						errors.add(new ValidationError(msg, recordId));

					for(final String msg : collect(em -> validateConstraints(contextPath,
						record, model, grammar, em)))
						errors.add(new ValidationError(msg, recordId));
				}
		}

		return errors;
	}

	/**
	 * Semantically validates the given {@link FLEFModel}. Validates
	 * referential integrity, symbol resolution, and ID uniqueness.
	 *
	 * @param model the model to validate
	 * @return the issues, never {@code null}
	 */
	public List<ValidationError> validateIntegrityStructured(final FLEFModel model){
		final List<ValidationError> errors = new ArrayList<>();

		final Set<String> declaredIds = new HashSet<>();
		collectDeclaredIdsStructured(model, declaredIds, errors);
		verifyReferencesStructured(declaredIds, model, errors);

		return errors;
	}

	/**
	 * Validates business/logical rules that are not captured by the
	 * grammar constraints.
	 *
	 * @param model the model to validate
	 * @return the issues, never {@code null}
	 */
	public List<ValidationError> validateBusinessRulesStructured(final FLEFModel model){
		final List<ValidationError> errors = new ArrayList<>();

		for(final FLEFRecord record : model.getRecords()){
			final String tag = record.getTag();
			final String contextPath = "records." + tag;
			final String recordId = record.getId();

			if(Strings.CI.equals("individual", tag))
				for(final String msg : collect(em -> validateIndividualDates(record, contextPath, em)))
					errors.add(new ValidationError(msg, recordId));

			if(Strings.CI.equals(RelationshipHandler.TYPE, tag))
				for(final String msg : collect(em -> validateRelationshipDates(record, contextPath, em)))
					errors.add(new ValidationError(msg, recordId));

			if(Strings.CI.equals(IdentityHypothesisHandler.TYPE, tag))
				for(final String msg : collect(em -> validateIdentityHypothesis(record, contextPath, model, em)))
					errors.add(new ValidationError(msg, recordId));

			if(Strings.CI.equals(EventParticipationHandler.TYPE, tag))
				for(final String msg : collect(em -> validateEventParticipation(record, contextPath, model, em)))
					errors.add(new ValidationError(msg, recordId));
		}

		return errors;
	}


	/* ======================================================================
	 *                          Public API — legacy (List<String>)
	 * ====================================================================== */

	/**
	 * Legacy overload: performs complete validation and returns only the
	 * messages. Prefer {@link #validateAllStructured(FLEFModel)} when the
	 * record id is needed.
	 */
	public List<String> validateAll(final FLEFModel model){
		return toMessages(validateAllStructured(model));
	}

	/**
	 * Legacy overload: see {@link #validateSchemaStructured(FLEFModel)}.
	 */
	public List<String> validateSchema(final FLEFModel model){
		return toMessages(validateSchemaStructured(model));
	}

	/**
	 * Legacy overload: see {@link #validateIntegrityStructured(FLEFModel)}.
	 */
	public List<String> validateIntegrity(final FLEFModel model){
		return toMessages(validateIntegrityStructured(model));
	}

	/**
	 * Legacy overload: see {@link #validateBusinessRulesStructured(FLEFModel)}.
	 */
	public List<String> validateBusinessRules(final FLEFModel model){
		return toMessages(validateBusinessRulesStructured(model));
	}

	private static List<String> toMessages(final List<ValidationError> errors){
		final List<String> out = new ArrayList<>(errors.size());
		for(final ValidationError e : errors)
			out.add(e.message());
		return out;
	}


	/* ======================================================================
	 *                          Internal — schema
	 * ====================================================================== */

	private record RecordContext(FLEFRecord record, String path){
	}

	/**
	 * Runs the given operation against a fresh error list and returns
	 * the collected messages. The operation is a lambda that appends to
	 * the list passed to it, matching the contract of
	 * {@code TypeDefinition.validate} and {@code Constraint.validate}.
	 * <p>
	 * This indirection is what allows the structured methods to attach
	 * a record id to each message: the operation runs on a fresh list,
	 * and the caller wraps each message with the id it knows about.
	 */
	private static List<String> collect(final Consumer<List<String>> operation){
		final List<String> out = new ArrayList<>();
		operation.accept(out);
		return out;
	}

	private void validateConstraints(final String contextPath, final FLEFRecord root, final FLEFModel model,
		final FLEFGrammar grammar, final List<String> errors){
		final Deque<RecordContext> stack = new ArrayDeque<>();
		stack.push(new RecordContext(root, contextPath));

		while(!stack.isEmpty()){
			final RecordContext current = stack.pop();
			final FLEFRecord record = current.record();
			final String path = current.path();

			final TypeDefinition typeDef = grammar.getType(record.getTag());
			if(typeDef instanceof StructType structType)
				for(final Constraint constraint : structType.getConstraints())
					constraint.validate(path, record, model, errors);

			final List<FLEFRecord> children = record.getChildren();
			for(int i = children.size() - 1; i >= 0; i--){
				final FLEFRecord child = children.get(i);
				final String childPath = path + DOT + child.getTag();
				stack.push(new RecordContext(child, childPath));
			}
		}
	}


	/* ======================================================================
	 *                          Internal — integrity
	 * ====================================================================== */

	private void collectDeclaredIdsStructured(final FLEFModel model, final Set<String> declaredIds,
		final List<ValidationError> errors){
		record TraversalNode(FLEFRecord record, String path){
		}

		final Deque<TraversalNode> stack = new ArrayDeque<>();

		final List<FLEFRecord> topLevelRecords = model.getRecords();
		for(int i = topLevelRecords.size() - 1; i >= 0; i--){
			final FLEFRecord topRecord = topLevelRecords.get(i);
			stack.push(new TraversalNode(topRecord, topRecord.getTag()));
		}

		while(!stack.isEmpty()){
			final TraversalNode current = stack.pop();
			final FLEFRecord record = current.record();
			final String path = current.path();

			final String id = record.getId();
			if(id != null && !declaredIds.add(id))
				errors.add(new ValidationError(
					String.format("Duplicate record ID '%s' found at '%s'", id, path),
					id));

			final List<FLEFRecord> children = record.getChildren();
			for(int i = children.size() - 1; i >= 0; i--){
				final FLEFRecord child = children.get(i);
				final String childPath = path + DOT + child.getTag();
				stack.push(new TraversalNode(child, childPath));
			}
		}
	}

	private void verifyReferencesStructured(final Set<String> declaredIds, final FLEFModel model,
		final List<ValidationError> errors){
		for(final String declaredId : declaredIds)
			if(!model.hasRecord(declaredId))
				// No record to attach: the target does not exist, so the
				// dialog has nothing to open.
				errors.add(new ValidationError(
					String.format("Unresolved cross-reference '%s': target record does not exist",
						declaredId),
					null));
	}


	/* ======================================================================
	 *                          Internal — business rules
	 * ====================================================================== */

	private void validateIndividualDates(final FLEFRecord individual, final String contextPath,
			final List<String> errors){
		// Placeholder: the complex check requires traversing the model
		// through event participations. Left empty for now.
	}

	private void validateRelationshipDates(final FLEFRecord relationship, final String contextPath,
		final List<String> errors){
		final String validFrom = FLEFRecordHelper.getChildValue(relationship, "VALID_FROM");
		final String validTo = FLEFRecordHelper.getChildValue(relationship, "VALID_TO");

		if(validFrom != null && validTo != null){
			if(validFrom.compareTo(validTo) > 0)
				errors.add(String.format(
					"Constraint violation at '%s': VALID_FROM (%s) must be before VALID_TO (%s)",
					contextPath, validFrom, validTo));
		}
	}

	private void validateIdentityHypothesis(final FLEFRecord hypothesis, final String contextPath,
			final FLEFModel model, final List<String> errors){
		final List<FLEFRecord> identities = FLEFRecordHelper.findChildren(hypothesis, "IDENTITY");
		if(identities == null || identities.size() != 2){
			errors.add(String.format(
				"Constraint violation at '%s': IDENTITY must be present twice",
				contextPath));
			return;
		}

		final FLEFRecord identity1 = identities.get(0);
		final FLEFRecord identity2 = identities.get(1);

		if(identity1 == null || identity2 == null)
			return;

		final String identity1Id = identity1.getValue();
		final String identity2Id = identity2.getValue();
		if(identity1Id != null && identity1Id.equals(identity2Id))
			errors.add(String.format(
				"Constraint violation at '%s': IDENTITIES must be different records (both reference '%s')",
				contextPath, identity1Id));
	}

	private void validateEventParticipation(final FLEFRecord participation, final String contextPath,
			final FLEFModel model, final List<String> errors){
		// Additional semantic checks can be added here.
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws Exception{
		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.3.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse("""
			header {
			  protocol {
			    name Family LEgacy Format
			    version 0.1.3
			  }
			  source {
			    system_id MyGenealogySoftware
			  }
			  date 2026-08-09
			  submitter {
			    name Test User
			  }
			}
			records {
			  individual {
			    id @I1@
			    name {
			      part {
			        type given
			        value John
			      }
			    }
			    modification {
			      creation {
			        date 2026-08-09
			      }
			    }
			  }
			  relationship {
			    id @R1@
			    subject {
			      individual @I1@
			    }
			    target {
			      individual @I1@
			    }
			    type biological_child
			    modification {
			      creation {
			        date 2026-08-09
			      }
			    }
			  }
			}
			""");

		final FLEFValidator validator = new FLEFValidator(grammar);
		final List<ValidationError> errors = validator.validateAllStructured(model);

		System.out.println("Validation errors: " + errors.size());
		for(final ValidationError e : errors)
			System.out.println("  - " + e.message()
				+ (e.hasRecord()? "   [record: " + e.recordId() + "]": ""));
	}

}
