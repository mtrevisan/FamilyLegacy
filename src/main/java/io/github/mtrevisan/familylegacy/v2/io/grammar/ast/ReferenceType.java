package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;


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
	public String toString(){
		return (voidable? "XrefOrVoid<": "Xref<") + targetTypeName + ">";
	}

}
