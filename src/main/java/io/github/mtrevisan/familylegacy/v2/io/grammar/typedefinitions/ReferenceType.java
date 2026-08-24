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
package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.Strings;

import java.util.List;


/**
 * {@code Xref&lt;Target&gt;} or {@code XrefOrVoid&lt;Target&gt;}.
 */
public final class ReferenceType extends TypeDefinition{

	private static final String TAG_VOID = "VOID";


	private final String targetTypeName;
	private final boolean voidable;


	public ReferenceType(final String name, final String targetTypeName, final boolean voidable){
		super(name);

		this.targetTypeName = targetTypeName;
		this.voidable = voidable;
	}


	public String getTargetTypeName(){
		return targetTypeName;
	}

	public boolean isVoidable(){
		return voidable;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		// 1. Syntactic validation: Check if node is a reference format
		final FLEFRecord referencedRecord = model.getRecordById(record.getValue());
		if(referencedRecord == null){
			errors.add(String.format("Expected cross-reference at '%s' for record %s", contextPath, record));

			return;
		}

		// 2. Syntactic validation: Check voidability
		// A valid VOID reference has no target ID to resolve
		if(Strings.CI.equals(TAG_VOID, record.getTag()) && !voidable)
			errors.add(String.format("Void reference not allowed at '%s'", contextPath));
	}

	@Override
	public String toString(){
		return (voidable? "XrefOrVoid<": "Xref<") + targetTypeName + ">";
	}

}
