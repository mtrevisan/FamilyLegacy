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
package io.github.mtrevisan.familylegacy.io.model;


import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import java.util.ServiceLoader;


/**
 * A service that tells how a record is named to the user (the handlers of the application know it, the lower layers do not depend on
 * them). It is provided by the UI.
 */
public interface RecordDisplay{

	/**
	 * @param record	The record to describe.
	 * @param model	The model the record belongs to.
	 * @return	The text that identifies the record to the user.
	 */
	String text(FLEFRecord record, FLEFModel model);

	/**
	 * @param record	The record to describe.
	 * @param model	The model the record belongs to.
	 * @return	The text that identifies the record to the user, its ID if nothing provides it or the provider fails.
	 */
	static String describe(final FLEFRecord record, final FLEFModel model){
		final String fallback = (record.getId() != null? record.getId(): "?");
		final RecordDisplay display = Holder.INSTANCE;
		if(display == null)
			return fallback;

		try{
			final String text = display.text(record, model);
			return (text != null && !text.isBlank()? text: fallback);
		}
		catch(final RuntimeException ignored){
			return fallback;
		}
	}


	final class Holder{
		private static final RecordDisplay INSTANCE = ServiceLoader.load(RecordDisplay.class).findFirst().orElse(null);

		private Holder(){}
	}

}
