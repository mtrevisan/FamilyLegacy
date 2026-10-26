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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;

import java.util.Map;
import java.util.Set;


/**
 * Read access to the parts of the FLEF grammar the merger depends on: the record kinds, the struct type of each kind, and the
 * cardinality of each field.
 * <p>
 * The merger is schema-driven so that it follows the protocol (a singular field is never assigned twice, a repeated field is
 * united) and keeps working when new MINOR versions add fields.
 */
final class GrammarSupport{

	/** Types whose fields hold the evidence and the bookkeeping of an assertion, not the assertion itself. */
	static final Set<String> EVIDENCE_TYPES = Set.of("SourceCitation", "NoteStructure", "AuditStructure", "PrivacyStructure",
		"Confidence");

	static final String TYPE_AUDIT = "AuditStructure";
	static final String TYPE_PRIVACY = "PrivacyStructure";


	private final FLEFGrammar grammar;
	private final Map<String, TypeDefinition> recordKinds;


	GrammarSupport(final FLEFGrammar grammar){
		if(!(grammar.getType(XrefPathIndex.RECORD_UNION_NAME) instanceof UnionType recordUnion))
			throw new IllegalArgumentException("Grammar without the '" + XrefPathIndex.RECORD_UNION_NAME + "' union");

		this.grammar = grammar;
		recordKinds = recordUnion.getChoices();

		XrefPathIndex.initialize(grammar);
	}


	/** Record kinds (e.g. {@code individual}, {@code place_relationship}) in grammar order. */
	Set<String> recordKinds(){
		return recordKinds.keySet();
	}

	/**
	 * @return the struct type of a record kind, or {@code null} if the kind is unknown to this grammar
	 */
	StructType recordType(final String kind){
		return (kind != null && resolve(recordKinds.get(kind)) instanceof StructType struct? struct: null);
	}

	/**
	 * Follows aliases and named types down to their definition.
	 */
	TypeDefinition resolve(TypeDefinition type){
		while(type instanceof ScalarType scalar && grammar.hasType(scalar.getName())){
			final TypeDefinition next = grammar.getType(scalar.getName());
			if(next == type)
				break;

			type = next;
		}
		return type;
	}

	/**
	 * Name of the type of a field, as declared (e.g. {@code SourceCitation}), or {@code null} for an anonymous type.
	 */
	String declaredTypeName(final FieldDefinition field){
		final TypeDefinition type = field.type();
		if(type instanceof ScalarType scalar && grammar.hasType(scalar.getName()))
			return scalar.getName();
		return resolve(type).getName();
	}

	boolean isEvidenceField(final FieldDefinition field){
		final String typeName = declaredTypeName(field);
		return (typeName != null && EVIDENCE_TYPES.contains(typeName));
	}

	static boolean isRepeated(final FieldDefinition field){
		final Cardinality cardinality = field.cardinality();
		return (cardinality == Cardinality.ZERO_OR_MORE || cardinality == Cardinality.ONE_OR_MORE);
	}

	/**
	 * The struct type of a field, or {@code null} if the field is a leaf, a reference, or a union.
	 */
	StructType structOf(final FieldDefinition field){
		return (resolve(field.type()) instanceof StructType struct? struct: null);
	}

	UnionType unionOf(final FieldDefinition field){
		return (resolve(field.type()) instanceof UnionType union? union: null);
	}

	/**
	 * The struct type of an arm of a union, or {@code null} if the arm is a leaf or a reference.
	 */
	StructType structOfArm(final UnionType union, final String arm){
		return (resolve(union.getChoices().get(arm)) instanceof StructType struct? struct: null);
	}

}
