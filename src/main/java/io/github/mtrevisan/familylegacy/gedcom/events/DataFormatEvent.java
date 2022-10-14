/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom.events;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.function.Consumer;


/** Raised upon a data format error on a value has been detected. */
public class DataFormatEvent{

	public enum DataFormatErrorType{
		MANDATORY_FIELD_MISSING
	}


	private final DataFormatErrorType type;
	private final String reference;
	private final GedcomNode container;
	private final Consumer<Object> onCloseGracefully;


	public DataFormatEvent(final DataFormatErrorType type, final String reference, final GedcomNode container){
		this(type, reference, container, null);
	}

	public DataFormatEvent(final DataFormatErrorType type, final String reference, final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.type = type;
		this.reference = reference;
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;
	}

	public final DataFormatErrorType getType(){
		return type;
	}

	public final String getReference(){
		return reference;
	}

	public final GedcomNode getContainer(){
		return container;
	}

	public final Consumer<Object> getOnCloseGracefully(){
		return onCloseGracefully;
	}

	@Override
	public final String toString(){
		return "DataFormatEvent{" + "type=" + type + ", container=" + container + '}';
	}

}
