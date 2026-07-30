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
package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.List;


/**
 * Thrown when validation fails.
 */
public final class ValidationException extends Exception{

	private final List<ValidationError> errors;


	public static ValidationException create(final List<ValidationError> errors){
		return new ValidationException(errors);
	}


	private ValidationException(final List<ValidationError> errors){
		super("Validation failed with " + errors.size() + " error(s)");

		this.errors = errors;
	}


	public List<ValidationError> getErrors(){
		return errors;
	}


	@Override
	public String getMessage(){
		final StringBuilder sb = new StringBuilder(super.getMessage());
		for(final ValidationError e : errors)
			sb.append("\n  ")
				.append(e);
		return sb.toString();
	}

}
