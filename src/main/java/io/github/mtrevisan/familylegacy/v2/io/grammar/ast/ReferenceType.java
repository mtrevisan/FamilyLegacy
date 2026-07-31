package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * {@code Xref<Target>} or {@code XrefOrVoid<Target>}.
 */
public final class ReferenceType extends TypeDefinition{

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
	public void validate(final String contextPath, final FLEFRecord record, final FLEFGrammar grammar,
			final List<String> errors){
		if(!record.isReference()){
			errors.add(String.format("Expected cross-reference at '%s'", contextPath));

			return;
		}
		if(record.isVoid() && !voidable)
			errors.add(String.format("Void reference not allowed at '%s'", contextPath));
	}

	@Override
	public String toString(){
		return (voidable? "XrefOrVoid<": "Xref<") + targetTypeName + ">";
	}

}
