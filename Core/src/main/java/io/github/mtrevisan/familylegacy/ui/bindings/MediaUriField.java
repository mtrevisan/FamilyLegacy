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
package io.github.mtrevisan.familylegacy.ui.bindings;

import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;


/**
 * A text field holding the location of a media: it <em>shows</em> it as the user chose (see {@link MediaPathDisplay}), but
 * {@link #getText()} always gives the whole value, as stored.
 */
public class MediaUriField extends BoundTextField{

	/** The value as stored (what is shown may be shortened). */
	private String fullValue;


	public MediaUriField(final String path){
		super(path);
	}


	@Override
	public String getText(){
		return BindingsHelper.getText(fullValue);
	}

	@Override
	public void setText(final String value){
		super.setText(MediaPathDisplay.display(value));
		//(after the call, because an editable field comes back through `forceSetText`)
		fullValue = value;
	}

	@Override
	public void forceSetText(final String value){
		fullValue = value;

		super.forceSetText(MediaPathDisplay.display(value));
	}

}
