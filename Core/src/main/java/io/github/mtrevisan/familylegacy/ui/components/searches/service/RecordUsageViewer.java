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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.awt.Window;
import java.util.ServiceLoader;


/**
 * Where a record is used, as the selection dialog needs it before deleting a record: found as a service, because the dialog that
 * shows it lives with the editors, which depend on the selection dialog.
 */
public interface RecordUsageViewer{

	/** Shows where a record is used (the records that point to it), over the given window. */
	void show(Window parent, FLEFModel model, FLEFRecord record);

	/** @return the name of a record in a list: its type, what it is about, and its identifier */
	String describe(FLEFModel model, FLEFRecord record);

	/** @return the viewer of the application */
	static RecordUsageViewer find(){
		return ServiceLoader.load(RecordUsageViewer.class).findFirst()
			.orElseThrow(() -> new IllegalStateException("No viewer of the usage of a record is declared"));
	}

}
