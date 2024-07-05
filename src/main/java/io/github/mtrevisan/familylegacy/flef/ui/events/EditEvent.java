/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.events;

import java.util.Map;
import java.util.function.Consumer;


/** Raised upon an editing on a node are required. */
public class EditEvent{

	public enum EditType{
//		ASSERTION,
		CITATION, EXTRACT, LOCALIZED_EXTRACT,
		SOURCE,
		REPOSITORY,

		DATE,
		CALENDAR,

		PLACE, LOCALIZED_PLACE_NAME,

		NOTE,

		MEDIA, PHOTO, PHOTO_CROP,

		PERSON,
		NAME, LOCALIZED_PERSON_NAME, LOCALIZED_PERSON_NAME_ALTERNATE,

//		GROUP,

//		EVENT,

		CULTURAL_NORM,

//		CONTACT,

		REFERENCE
	}


	private final EditType type;
	private final Map<String, Object> container;
	private Consumer<Object> onCloseGracefully;


	public static EditEvent create(final EditType type, final Map<String, Object> container){
		return new EditEvent(type, container);
	}


	private EditEvent(final EditType type, final Map<String, Object> container){
		this.type = type;
		this.container = container;
	}


	public EditEvent withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;

		return this;
	}

	public final EditType getType(){
		return type;
	}

	public final Map<String, Object> getContainer(){
		return container;
	}

	public final Consumer<Object> getOnCloseGracefully(){
		return onCloseGracefully;
	}

	@Override
	public final String toString(){
		return "EditEvent{" + "type=" + type + ", container=" + container + '}';
	}

}
