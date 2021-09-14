/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom;


import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;


public final class GedcomNodeFlef extends GedcomNode{

	private static final String TAG_NEW_LINE = "NEW_LINE";


	@Override
	protected GedcomNode createNewNodeWithTag(final String tag){
		return GedcomNodeBuilder.create(Protocol.FLEF, tag);
	}

	@Override
	public String getValue(){
		if(children != null){
			final StringBuilder sb = new StringBuilder();
			if(value != null)
				sb.append(value);
			for(final GedcomNode child : children)
				if(TAG_NEW_LINE.equals(child.tag)){
					sb.append(NEW_LINE);
					if(child.value != null)
						sb.append(child.value);
				}
			return (!sb.isEmpty()? sb.toString(): null);
		}
		else
			return value;
	}

	@Override
	public GedcomNodeFlef withValue(final String value){
		if(value != null && !value.isEmpty()){
			//split line into CONTINUATION if applicable
			int offset = 0;
			int cutIndex;
			final int length = value.length();
			while((cutIndex = value.indexOf(NEW_LINE, offset)) >= 0){
				final String subValue = value.substring(offset, cutIndex);
				addValue(TAG_NEW_LINE, subValue);

				offset = cutIndex + 1;
			}

			if(offset < length){
				if(offset == 0)
					this.value = value;
				else
					addValue(TAG_NEW_LINE, value.substring(offset));
			}
		}
		return this;
	}

}