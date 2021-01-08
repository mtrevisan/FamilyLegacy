/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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


/** Raised upon an editing on a node are required. */
public class EditEvent{

	public enum EditType{
		INDIVIDUAL, INDIVIDUAL_CITATION,
		FAMILY, FAMILY_CITATION,
		GROUP, GROUP_CITATION,
		NOTE, NOTE_CITATION,
		SOURCE, SOURCE_CITATION,
		CUTOUT,
		EVENT, EVENT_CITATION,
		PLACE, PLACE_CITATION,
		REPOSITORY, REPOSITORY_CITATION,
		CONTACT,
		CULTURAL_RULE, CULTURAL_RULE_CITATION,
		DOCUMENT, DOCUMENT_CITATION
	}


	private final EditType type;
	private final GedcomNode container;
	private final Consumer<Object> onCloseGracefully;


	public EditEvent(final EditType type, final GedcomNode container){
		this(type, container, null);
	}

	public EditEvent(final EditType type, final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.type = type;
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;
	}

	public EditType getType(){
		return type;
	}

	public GedcomNode getContainer(){
		return container;
	}

	public Consumer<Object> getOnCloseGracefully(){
		return onCloseGracefully;
	}

}
