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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class GedcomNodeBuilder{

	/** NOTE: {@link Pattern#DOTALL} is for unicode line separator. */
	private static final Pattern GEDCOM_LINE = Pattern.compile("^\\s*(\\d)\\s+(@([^@ ]+)@\\s+)?([a-zA-Z_0-9.]+)(\\s+@([^@ ]+)@)?(\\s(.*))?$",
		Pattern.DOTALL);
	private static final int GEDCOM_LINE_LEVEL = 1;
	private static final int GEDCOM_LINE_ID = 3;
	private static final int GEDCOM_LINE_TAG = 4;
	private static final int GEDCOM_LINE_XREF = 6;
	private static final int GEDCOM_LINE_VALUE = 8;


	public static GedcomNode createRoot(final Protocol protocol){
		final GedcomNode root = createEmpty(protocol);
		root.level = -1;
		return root;
	}

	public static GedcomNode createEmpty(final Protocol protocol){
		if(protocol == Protocol.GEDCOM)
			return new GedcomNodeGedcom();
		else if(protocol == Protocol.FLEF)
			return new GedcomNodeFlef();
		//cannot happen
		return null;
	}

	public static GedcomNode create(final Protocol protocol, final String tag){
		if(tag == null || tag.isEmpty())
			throw new IllegalArgumentException("Tag must be present");

		return createEmpty(protocol)
			.withTag(tag);
	}

	public static GedcomNode create(final Protocol protocol, final String tag, final String id, final String value){
		if(id == null || id.isEmpty())
			throw new IllegalArgumentException("ID must be present");

		return create(protocol, tag)
			.withID(id)
			.withValue(value);
	}

	public static GedcomNode createCloneWithoutID(final Protocol protocol, final GedcomNode node){
		final GedcomNode clone = create(protocol, node.getTag())
			.withXRef(node.getXRef())
			.withValue(node.getValue());
		for(final GedcomNode child : node.getChildren())
			clone.addChild(createCloneWithID(protocol, child));
		return clone;
	}

	private static GedcomNode createCloneWithID(final Protocol protocol, final GedcomNode node){
		final GedcomNode clone = create(protocol, node.getTag())
			.withID(node.getID())
			.withXRef(node.getXRef())
			.withValue(node.getValue());
		for(final GedcomNode child : node.getChildren())
			clone.addChild(createCloneWithID(protocol, child));
		return clone;
	}

	public static GedcomNode parse(final Protocol protocol, final CharSequence line){
		final Matcher m = GEDCOM_LINE.matcher(line);
		if(!m.find())
			return null;

		return createEmpty(protocol)
			.withLevel(m.group(GEDCOM_LINE_LEVEL))
			.withID(m.group(GEDCOM_LINE_ID))
			.withTag(m.group(GEDCOM_LINE_TAG))
			.withXRef(m.group(GEDCOM_LINE_XREF))
			.withValue(m.group(GEDCOM_LINE_VALUE));
	}

}