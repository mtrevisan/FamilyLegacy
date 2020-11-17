package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.services.ReflectionHelper;

import java.util.List;


public abstract class Transformation<FROM extends Store, TO extends Store>{

	private final Protocol protocolFrom;
	private final Protocol protocolTo;

	protected final Transformer transformerFrom;
	protected final Transformer transformerTo;


	@SuppressWarnings("unchecked")
	protected Transformation(){
		final List<Class<?>> generics = ReflectionHelper.resolveGenericTypes(getClass(), Transformation.class);

		protocolFrom = Protocol.fromStore((Class<? extends Store>)generics.get(0));
		protocolTo = Protocol.fromStore((Class<? extends Store>)generics.get(1));

		transformerFrom = protocolFrom.transformer;
		transformerTo = protocolTo.transformer;
	}

	public abstract void to(final FROM origin, final TO destination);

	public abstract void from(final TO origin, final FROM destination) throws GedcomGrammarParseException;

}
