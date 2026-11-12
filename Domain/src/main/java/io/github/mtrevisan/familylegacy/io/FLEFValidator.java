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
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FileDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ReferenceType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;


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

	private static final String FIELD_HEADER = FLEFGrammarParser.FIELD_HEADER;

	/** The share of the work that the schema is, and the share done when the integrity is: the business rules are the rest. */
	private static final double PROGRESS_SCHEMA = 0.7;
	private static final double PROGRESS_INTEGRITY = 0.85;
	/** The records validated between two reports of the progress. */
	private static final int PROGRESS_RECORDS = 50;

	/** The kind of the issue of a relationship that ends before it begins. */
	public static final String KIND_VALIDITY = "validity_dates";

	/** How serious a validation issue is. */
	public enum Severity{
		/** Something to look at: the file is still valid. */
		WARNING,
		/** Something wrong: the file is not valid. */
		ERROR
	}


	/**
	 * A single validation issue.
	 *
	 * @param message  the human-readable description of the problem
	 * @param recordId the id of the record the issue is about, or
	 *                 {@code null} when the issue is not tied to a
	 *                 specific record (e.g. a header-level error, or an
	 *                 unresolved cross-reference whose target does not
	 *                 exist)
	 * @param severity how serious it is: an {@link Severity#ERROR error} makes the file invalid, a {@link Severity#WARNING warning}
	 *                 is something to look at (e.g. an individual without a name)
	 * @param kind     what kind of problem it is, a stable key ({@code circular_parentage}...) that the report groups by; empty when
	 *                 it is not known (what the grammar finds: the report then groups by the type of the record)
	 */
	public record ValidationError(String message, String recordId, Severity severity, String kind){

		public ValidationError{
			Objects.requireNonNull(message, "message must not be null");
			Objects.requireNonNull(severity, "severity must not be null");
			kind = (kind != null? kind: "");
		}

		/** An issue of an unknown kind. */
		public ValidationError(final String message, final String recordId, final Severity severity){
			this(message, recordId, severity, "");
		}

		/** An error: what the grammar says is wrong is an error. */
		public ValidationError(final String message, final String recordId){
			this(message, recordId, Severity.ERROR);
		}

		/** {@code true} when the error can be opened in the editor. */
		public boolean hasRecord(){
			return (recordId != null);
		}
	}


	private final FLEFGrammar grammar;


	public FLEFValidator(final FLEFGrammar grammar){
		this.grammar = grammar;
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
		return validateAllStructured(model, fraction -> {});
	}

	/**
	 * The same, telling how far it got. The progress is the share of the work done, from 0 to 1, and never goes back: the schema, which
	 * visits every record, is the first 70 % (by the number of records), the integrity 15 %, the business rules the rest. It is called
	 * from the thread that validates. If that thread is interrupted the validation stops with what it found.
	 *
	 * @param model    the model to validate; may be {@code null}
	 * @param progress told, from time to time, the share of the work done (0 to 1)
	 * @return the issues, never {@code null}
	 */
	public List<ValidationError> validateAllStructured(final FLEFModel model, final DoubleConsumer progress){
		final List<ValidationError> errors = new ArrayList<>();

		errors.addAll(validateSchemaStructured(model, fraction -> progress.accept(fraction * PROGRESS_SCHEMA)));
		progress.accept(PROGRESS_SCHEMA);

		if(errors.isEmpty() && !Thread.currentThread().isInterrupted()){
			errors.addAll(validateIntegrityStructured(model));
			progress.accept(PROGRESS_INTEGRITY);
		}

		if(errors.isEmpty() && !Thread.currentThread().isInterrupted())
			errors.addAll(validateBusinessRulesStructured(model));

		progress.accept(1.);
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
		return validateSchemaStructured(model, fraction -> {});
	}

	/** The same, telling how far it got through the records (0 to 1). */
	public List<ValidationError> validateSchemaStructured(final FLEFModel model, final DoubleConsumer progress){
		if(model == null)
			return List.of(new ValidationError("Model is null", null));

		final FileDefinition fileDef = grammar.getFileDefinition();
		final List<ValidationError> errors = new ArrayList<>();

		// Header: no record id to attach, the header is not a record.
		final TypeDefinition headerTypeRef = grammar.getHeaderType();
		if(headerTypeRef != null && model.getHeader() != null){
			final TypeDefinition headerType = (grammar.hasType(headerTypeRef.getName())
				? grammar.getType(headerTypeRef.getName())
				: headerTypeRef);
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
		if(fileDef != null && fileDef.recordsField() != null){
			final TypeDefinition recordsType = grammar.getType(fileDef.recordsField().type().getName());
			if(recordsType != null){
				final List<FLEFRecord> records = model.getRecords();
				int done = 0;
				for(final FLEFRecord record : records){
					if(Thread.currentThread().isInterrupted())
						break;

					final String contextPath = FLEFRecordHelper.composePath(FLEFGrammarParser.FIELD_RECORDS, record.getTag());
					final String recordId = record.getId();

					for(final String msg : collect(em -> recordsType.validate(contextPath,
						record, model, grammar, em)))
						errors.add(new ValidationError(msg, recordId));

					for(final String msg : collect(em -> validateConstraints(contextPath,
						record, model, grammar, em)))
						errors.add(new ValidationError(msg, recordId));

					done ++;
					if(done % PROGRESS_RECORDS == 0)
						progress.accept((double)done / records.size());
				}
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
		for(final String duplicateId : model.getDuplicateIds())
			errors.add(new ValidationError(String.format("Duplicate record ID '%s' found at parse time: the last one wins", duplicateId),
				duplicateId));
		verifyReferencesStructured(model, errors);

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

		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records){
			final String tag = record.getTag();
			final String contextPath = "records." + tag;
			final String recordId = record.getId();

			if(RelationshipReader.isOfType(record))
				for(final String msg : collect(em -> validateValidity(record, RelationshipReader.TAG_VALID_FROM,
						RelationshipReader.TAG_VALID_TO, contextPath, em)))
					errors.add(new ValidationError(msg, recordId, Severity.ERROR, KIND_VALIDITY));

			if(PlaceRelationshipReader.isOfType(record))
				for(final String msg : collect(em -> validateValidity(record, PlaceRelationshipReader.TAG_VALID_FROM,
						PlaceRelationshipReader.TAG_VALID_TO, contextPath, em)))
					errors.add(new ValidationError(msg, recordId, Severity.ERROR, KIND_VALIDITY));

			if(IdentityHypothesisReader.isOfType(record))
				for(final String msg : collect(em -> validateIdentityHypothesis(record, contextPath, model, em)))
					errors.add(new ValidationError(msg, recordId));

			if(EventParticipationReader.isOfType(record))
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
			if(typeDef instanceof StructType structType){
				final List<Constraint> constraints = structType.getConstraints();
				for(final Constraint constraint : constraints)
					constraint.validate(path, record, model, errors);
			}

			final List<FLEFRecord> children = record.getChildren();
			for(int i = children.size() - 1; i >= 0; i --){
				final FLEFRecord child = children.get(i);
				final String childPath = FLEFRecordHelper.composePath(path, child.getTag());
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
		for(int i = topLevelRecords.size() - 1; i >= 0; i --){
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
			for(int i = children.size() - 1; i >= 0; i --){
				final FLEFRecord child = children.get(i);
				final String childPath = FLEFRecordHelper.composePath(path, child.getTag());
				stack.push(new TraversalNode(child, childPath));
			}
		}
	}

	/**
	 * Checks every reference (a field whose grammar type is an {@code Xref<...>}, wherever it is: nested, in a oneof, in an
	 * alternation) of every top-level record against the records of the model. The ids that a nested record declares are not
	 * references, and are not checked.
	 */
	private void verifyReferencesStructured(final FLEFModel model, final List<ValidationError> errors){
		final FileDefinition fileDef = grammar.getFileDefinition();
		if(fileDef == null || fileDef.recordsField() == null)
			return;

		final TypeDefinition recordsType = resolveType(fileDef.recordsField().type());
		if(recordsType == null)
			return;

		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records){
			final Set<String> unresolved = new LinkedHashSet<>();
			final String path = FLEFRecordHelper.composePath(FLEFGrammarParser.FIELD_RECORDS, record.getTag());
			collectUnresolvedReferences(record, recordsType, path, model, unresolved);

			for(final String message : unresolved)
				errors.add(new ValidationError(message, record.getId()));
		}
	}

	private TypeDefinition resolveType(TypeDefinition type){
		//resolve the symbolic aliases
		int guard = 0;
		while(type instanceof ScalarType scalar && grammar.hasType(scalar.getName()) && guard ++ < 32)
			type = grammar.getType(scalar.getName());
		return type;
	}

	private void collectUnresolvedReferences(final FLEFRecord record, final TypeDefinition typeDefinition, final String path,
			final FLEFModel model, final Set<String> unresolved){
		final TypeDefinition type = resolveType(typeDefinition);
		if(type instanceof ReferenceType reference){
			final String target = record.getValue();
			if(StringUtils.isEmpty(target)){
				if(!reference.isVoidable())
					unresolved.add(String.format("Unresolved cross-reference at '%s': no target id", path));
			}
			else if(!model.hasRecord(target))
				unresolved.add(String.format("Unresolved cross-reference '%s' at '%s': target record does not exist", target, path));
		}
		else if(type instanceof StructType struct){
			for(final FLEFRecord child : record.getChildren()){
				final FieldDefinition field = struct.getField(child.getTag());
				if(field != null)
					collectUnresolvedReferences(child, field.type(), FLEFRecordHelper.composePath(path, field.name()), model,
						unresolved);
			}
		}
		else if(type instanceof UnionType union){
			final Map<String, TypeDefinition> choices = union.getChoices();
			FLEFRecord target = record;
			String tag = record.getTag();
			if(!choices.containsKey(tag) && !record.getChildren().isEmpty()){
				final FLEFRecord firstChild = record.getTheOnlyChild();
				if(firstChild != null && !firstChild.isEmpty() && choices.containsKey(firstChild.getTag())){
					target = firstChild;
					tag = firstChild.getTag();
				}
			}
			final TypeDefinition choice = choices.get(tag);
			if(choice != null)
				collectUnresolvedReferences(target, choice, (target == record? path: FLEFRecordHelper.composePath(path, tag)), model,
					unresolved);
		}
		else if(type instanceof AlternationType alternation)
			for(final TypeDefinition alternative : alternation.getAlternatives())
				collectUnresolvedReferences(record, alternative, path, model, unresolved);
	}


	/* ======================================================================
	 *                          Internal — business rules
	 * ====================================================================== */

	/**
	 * A relationship that ends before it begins: every day that {@code valid_to} may stand for is before every day that
	 * {@code valid_from} may stand for. The dates are normalized, whatever their calendar and precision (a year, an approximate date, a
	 * bounded one), and a date that cannot be read is not judged: the comparison is of the days, not of the text, which gave a wrong
	 * answer for two dates written in two ways.
	 */
	private void validateValidity(final FLEFRecord record, final String fromTag, final String toTag, final String contextPath,
		final List<String> errors){
		final FLEFRecord from = FLEFRecordHelper.findChild(record, fromTag);
		final FLEFRecord to = FLEFRecordHelper.findChild(record, toTag);
		if(from == null || to == null)
			return;

		try{
			final DateRange validFrom = DateRange.of(DateNormalizer.normalize(from));
			final DateRange validTo = DateRange.of(DateNormalizer.normalize(to));
			if(validFrom != null && validTo != null && validTo.isBefore(validFrom))
				errors.add(String.format("Constraint violation at '%s': %s must not be before %s", contextPath,
					toTag.toUpperCase(Locale.ROOT), fromTag.toUpperCase(Locale.ROOT)));
		}
		catch(final RuntimeException ignored){
			//a date that cannot be normalized is checked where it is edited, not here
		}
	}

	private void validateIdentityHypothesis(final FLEFRecord hypothesis, final String contextPath,
			final FLEFModel model, final List<String> errors){
		final List<FLEFRecord> identities = FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY);
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
		//the grammar does not enforce: the role `property` is for the place that is the object of the event, so the participant MUST be
		//a place; every other (known) role is for an individual or a group
		final FLEFRecord role = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_ROLE);
		final FLEFRecord participant = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
		if(role == null || participant == null || participant.getChildren().size() != 1)
			return;

		final String roleValue = role.getValue();
		final String kind = participant.getTheOnlyChild().getTag();
		if(StringUtils.isEmpty(roleValue) || kind == null)
			return;

		final boolean placeParticipant = EventParticipationReader.TAG_PARTICIPANT_PLACE.equalsIgnoreCase(kind);
		final boolean propertyRole = EventParticipationReader.ENUM_ROLE_PROPERTY.equalsIgnoreCase(roleValue);
		if(propertyRole && !placeParticipant)
			errors.add(String.format("Constraint violation at '%s': role '%s' requires the participant to be a place (found '%s')",
				contextPath, roleValue, kind));
		else if(!propertyRole && placeParticipant && Arrays.stream(EventParticipationReader.ROLES)
				.anyMatch(r -> r.equalsIgnoreCase(roleValue)))
			errors.add(String.format("Constraint violation at '%s': a place can only have the role '%s' (found '%s')", contextPath,
				EventParticipationReader.ENUM_ROLE_PROPERTY, roleValue));
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	static void main(final String[] args) throws Exception{
		final Path path = Paths.get("src/main/resources/gedg/flef_0.2.0.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse("""
			header {
			  protocol {
			    name Family LEgacy Format
			    version 0.2.0
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
				+ (e.hasRecord()? "   [record: " + e.recordId() + "]": StringUtils.EMPTY));
	}

}
