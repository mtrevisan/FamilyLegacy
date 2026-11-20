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
package io.github.mtrevisan.reports;


/**
 * Where an image is put on the page. Not every format can do all of them: the text formats (Markdown, plain text) ignore the alignment,
 * and a format that cannot wrap the text around an image (Word) puts it on the side.
 */
public enum ImageAlignment{

	/** What the format does by itself (centred in a page-based format, as a block in a web page). */
	DEFAULT(null),
	LEFT("text-align:left"),
	CENTER("text-align:center"),
	RIGHT("text-align:right"),
	/** On the left, with the text flowing around it. */
	WRAP_LEFT("float:left;margin:.3em 1em .5em 0;max-width:40%"),
	/** On the right, with the text flowing around it. */
	WRAP_RIGHT("float:right;margin:.3em 0 .5em 1em;max-width:40%");


	private final String style;


	ImageAlignment(final String style){
		this.style = style;
	}

	/** @return	The inline CSS of the figure that holds the image, {@code null} if there is none. */
	public String style(){
		return style;
	}

	/** @return	Whether the text flows around the image. */
	public boolean isWrapped(){
		return (this == WRAP_LEFT || this == WRAP_RIGHT);
	}

	/** @return	Whether the image is on the right side of the page. */
	public boolean isRight(){
		return (this == RIGHT || this == WRAP_RIGHT);
	}

}
