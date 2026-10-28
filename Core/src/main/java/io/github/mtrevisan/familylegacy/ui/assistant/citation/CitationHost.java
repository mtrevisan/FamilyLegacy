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
package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;


/** An editor of a citation that can be helped by the Source Citation Assistant. */
public interface CitationHost{

	/** @return the locator currently entered (can be {@code null} or empty) */
	String locator();

	/**
	 * Writes the locator in the editor. It is called only after the user asked for it; the record is saved only if the user
	 * saves the editor.
	 */
	void applyLocator(String locator);

	/** @return the source the citation refers to, if any and if it is already chosen (can be {@code null}) */
	FLEFRecord citedSource();

	/** @return whether the citation refers to a repository, that can be suggested by the assistant */
	default boolean acceptsRepository(){
		return false;
	}

	/** Writes the repository in the editor, after the user asked for it. */
	default void applyRepository(final FLEFRecord repository){}

}
