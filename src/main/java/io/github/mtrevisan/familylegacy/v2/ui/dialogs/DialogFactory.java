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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.awt.Dialog;


/**
 * A reference to a record dialog's private constructor, used by {@link BaseRecordDialog#createNew(Dialog, FLEFModel, DialogFactory)}
 * and {@link BaseRecordDialog#createEdit(Dialog, FLEFModel, FLEFRecord, DialogFactory)} to collapse the identical
 * {@code createNew}/{@code createEdit} boilerplate that used to be duplicated in every subclass.
 * <p>
 * Every record dialog's private constructor already has the shape {@code (Dialog, FLEFModel, FLEFRecord)}, so a
 * method reference such as {@code NoteRecordDialog::new} satisfies this interface directly.
 *
 * @param <T>	The concrete dialog type.
 */
@FunctionalInterface
public interface DialogFactory<T extends BaseRecordDialog>{

	T create(Dialog parent, FLEFModel model, FLEFRecord record);

}
