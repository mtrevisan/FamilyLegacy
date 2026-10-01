/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;


public final class ActionCommand{

	/** Raised upon changes on the number of persons in the store. */
	public static final Integer ACTION_COMMAND_PERSON = 0;
	/** Raised upon changes on the number of groups in the store. */
	public static final Integer ACTION_COMMAND_GROUP = 1;
	/** Raised upon changes on the number of events in the store. */
	public static final Integer ACTION_COMMAND_EVENT = 3;
	/** Raised upon changes on the number of places in the store. */
	public static final Integer ACTION_COMMAND_PLACE = 4;
	/** Raised upon changes on the number of notes in the store. */
	public static final Integer ACTION_COMMAND_NOTE = 5;
	/** Raised upon changes on the number of repositories in the store. */
	public static final Integer ACTION_COMMAND_REPOSITORY = 6;
	/** Raised upon changes on the number of cultural norms in the store. */
	public static final Integer ACTION_COMMAND_CULTURAL_NORM = 7;
	/** Raised upon changes on the number of sources in the store. */
	public static final Integer ACTION_COMMAND_SOURCE = 8;
	/** Raised upon changes on the number of calendar in the store. */
	public static final Integer ACTION_COMMAND_CALENDAR = 9;
	/** Raised upon changes on the number of historical events in the store. */
	public static final Integer ACTION_COMMAND_HISTORIC_EVENT = 10;
	/** Raised upon changes on the number of research statuses in the store. */
	public static final Integer ACTION_COMMAND_RESEARCH_STATUS = 11;


	private ActionCommand(){}

}
