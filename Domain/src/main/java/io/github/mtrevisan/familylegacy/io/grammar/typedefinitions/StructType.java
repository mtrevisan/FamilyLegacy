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
package io.github.mtrevisan.familylegacy.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * A group of fields (and optional constraints), either named ({@code struct Name { ... }}) or inline.
 */
public class StructType extends TypeDefinition{

	private final List<FieldDefinition> fields;
	private final List<Constraint> constraints;
	/** The position of each field by its name (in lowercase), to tell in one pass to which field each child of a record belongs. */
	private final Map<String, Integer> fieldPositions = new HashMap<>();


	public StructType(final String name, final List<FieldDefinition> fields, final List<Constraint> constraints){
		super(name);

		this.fields = List.copyOf(fields);
		this.constraints = List.copyOf(constraints);
		for(int i = 0; i < this.fields.size(); i ++)
			//the first field of a name wins, as the first match does when the fields are scanned
			fieldPositions.putIfAbsent(this.fields.get(i).name().toLowerCase(Locale.ROOT), i);
	}


	public List<FieldDefinition> getFields(){
		return fields;
	}

	public List<Constraint> getConstraints(){
		return constraints;
	}

	public FieldDefinition getField(final String name){
		for(final FieldDefinition f : fields)
			if(Strings.CI.equals(f.name(), name))
				return f;
		return null;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model,
			final FLEFGrammar grammar, final List<String> errors){
		validate(ValidationPath.of(contextPath), record, model, grammar, errors);
	}

	/**
	 * The children of the record are assigned to their fields in a single pass (not one pass, and one list, for each field), and the
	 * path of a field is written only if an error needs it.
	 */
	@Override
	public void validate(final ValidationPath path, final FLEFRecord record, final RecordLookup model, final FLEFGrammar grammar,
			final List<String> errors){
		final List<FLEFRecord> children = record.getChildren();
		final int childCount = children.size();
		final int fieldCount = fields.size();

		//the field of each child (-1 if it is of none), and how many children each field has
		final int[] fieldOfChild = new int[childCount];
		final int[] counts = new int[fieldCount];
		for(int i = 0; i < childCount; i ++){
			final String tag = children.get(i).getTag();
			final Integer position = (tag != null? fieldPositions.get(tag.toLowerCase(Locale.ROOT)): null);
			if(position != null){
				fieldOfChild[i] = position;
				counts[position] ++;
			}
			else
				fieldOfChild[i] = -1;
		}

		// Check all fields defined for this structure
		for(int f = 0; f < fieldCount; f ++){
			final FieldDefinition fieldDef = fields.get(f);
			final String fieldName = fieldDef.name();
			final int count = counts[f];

			// A field whose name is also the name of an earlier field has no children of its own (the earlier one takes them), as when
			// the children are looked for by name: all of them are the children of both
			final boolean shared = (fieldPositions.get(fieldName.toLowerCase(Locale.ROOT)) != f);
			final int matching = (shared? countByName(children, fieldName): count);

			// Validate cardinality constraints (?, *, +, required)
			if(!fieldDef.cardinality().isValidCount(matching)){
				errors.add(String.format("Missing required child '%s' under '%s' (found %d, expected %s), record %s",
					fieldName, path, matching, fieldDef.cardinality(), record));

				continue;
			}
			if(matching == 0)
				continue;

			// Resolve the field's declared type
			TypeDefinition fieldType = fieldDef.type();
			// If the type is a symbolic reference (ScalarType), dereference it against the grammar registry
			if(fieldType instanceof ScalarType scalar){
				final TypeDefinition resolved = grammar.getType(scalar.getName());
				if(resolved != null)
					fieldType = resolved;
			}

			// Delegate recursive validation of child records to the resolved type
			final ValidationPath fieldPath = path.child(fieldName);
			for(int i = 0; i < childCount; i ++)
				if(shared? Strings.CI.equals(children.get(i).getTag(), fieldName): fieldOfChild[i] == f)
					fieldType.validate(fieldPath, children.get(i), model, grammar, errors);
		}

		// Validate structural constraints (e.g., require one_of, require if)
		if(!constraints.isEmpty()){
			final String contextPath = path.toString();
			for(final Constraint constraint : constraints)
				constraint.validate(contextPath, record, model, errors);
		}
	}

	private static int countByName(final List<FLEFRecord> children, final String fieldName){
		int count = 0;
		for(int i = 0, size = children.size(); i < size; i ++)
			if(Strings.CI.equals(children.get(i).getTag(), fieldName))
				count ++;
		return count;
	}


	@Override
	public String toString(){
		return "struct" + (getName() != null? StringUtils.SPACE + getName(): StringUtils.EMPTY) + fields;
	}

}
