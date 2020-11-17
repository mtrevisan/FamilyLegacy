package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.services.ReflectionHelper;

import java.util.List;


public abstract class Transformation<FROM extends Store<FROM>, TO extends Store<TO>>{

	private static Protocol protocolFrom;
	private static Protocol protocolTo;

	protected static Transformer transformerFrom;
	protected static Transformer transformerTo;


	protected Transformation(){
		if(protocolFrom == null){
			final List<Class<?>> generics = ReflectionHelper.resolveGenericTypes(getClass(), Transformation.class);

			protocolFrom = Protocol.fromStore((Class<? extends Store<?>>)generics.get(0));
			protocolTo = Protocol.fromStore((Class<? extends Store<?>>)generics.get(1));

			transformerFrom = new Transformer(protocolFrom);
			transformerTo = new Transformer(protocolTo);
		}
	}

	public abstract void to(final FROM origin, final TO destination);

	public abstract void from(final TO origin, final FROM destination) throws GedcomGrammarParseException;

}
