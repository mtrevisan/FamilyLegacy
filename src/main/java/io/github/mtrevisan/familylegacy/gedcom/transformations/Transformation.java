/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

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

	public abstract void from(final TO origin, final FROM destination);

}
