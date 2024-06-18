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
package io.github.mtrevisan.familylegacy.flef.gedcom;

import io.github.mtrevisan.familylegacy.flef.helpers.StringHelper;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;


/**
 * Exception that occurred when parsing the grammar linked file.
 */
public final class GedcomGrammarException extends Exception{

	@Serial
	private static final long serialVersionUID = -9166944783238433522L;


	public static GedcomGrammarException create(final String message, final Object... parameters){
		return new GedcomGrammarException(StringHelper.format(message, parameters));
	}

	private GedcomGrammarException(final String message){
		super(message);
	}


	@Serial
	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@Serial
	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
