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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class Store{

	private static final Pattern PATTERN_ID = Pattern.compile("[^\\d]+([\\d]+)");

	private static final String CHARSET_X_MAC_ROMAN = "x-MacRoman";
	private static final String CRLF = StringUtils.CR + StringUtils.LF;


	GedcomNode root;
	protected String basePath;


	public Store load(final String grammarFile, final String gedcomFile) throws GedcomGrammarParseException, GedcomParseException{
		final GedcomGrammar grammar = GedcomGrammar.create(grammarFile);

		final GedcomNode root = GedcomParser.parse(gedcomFile, grammar);

		final int index = StringUtils.lastIndexOfAny(gedcomFile, "/", "\\");
		create(root, (index > 0? gedcomFile.substring(0, index): gedcomFile));

		return this;
	}

	protected void create(final GedcomNode root, final String basePath) throws GedcomParseException{
		Objects.requireNonNull(root, "Root cannot be null");
		Objects.requireNonNull(basePath, "Base path cannot be null");

		this.root = root;
		this.basePath = basePath;
	}

	int extractLastID(final CharSequence lastKey){
		final Matcher m = PATTERN_ID.matcher(lastKey);
		m.find();
		return Integer.parseInt(m.group(1));
	}

	public abstract Store transform();

	public void write(final OutputStream os) throws IOException{
		final String charset = getCharsetName();
		final String eol = (CHARSET_X_MAC_ROMAN.equals(charset)? StringUtils.CR: CRLF);
		final OutputStreamWriter writer = (AnselInputStreamReader.CHARACTER_ENCODING.equals(charset)?
			new AnselOutputStreamWriter(os): new OutputStreamWriter(os, charset));
		final Writer out = new BufferedWriter(writer);

		final Deque<GedcomNode> nodeStack = new LinkedList<>();
		//skip root node and add its children
		for(final GedcomNode child : root.getChildren())
			nodeStack.addLast(child);
		while(!nodeStack.isEmpty()){
			final GedcomNode child = nodeStack.pop();
			final List<GedcomNode> children = child.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				nodeStack.addFirst(children.get(i));

			out.write(Integer.toString(child.getLevel()));
			if(child.getLevel() == 0){
				appendID(out, child.getID());
				appendElement(out, child.getTag());
			}
			else{
				appendElement(out, child.getTag());
				appendID(out, child.getXRef());
				appendID(out, child.getID());
			}
			if(child.getValue() != null)
				appendElement(out, child.getRawValue());
			out.write(eol);
		}
		out.flush();
	}

	protected abstract String getCharsetName();

	private void appendID(final Writer out, final String id) throws IOException{
		if(id != null){
			out.write(' ');
			out.write('@');
			out.write(id);
			out.write('@');
		}
	}

	private void appendElement(final Writer out, final String elem) throws IOException{
		if(elem != null){
			out.write(' ');
			out.write(elem);
		}
	}

	static TreeMap<String, GedcomNode> generateIndexes(final Iterable<GedcomNode> list){
		final TreeMap<String, GedcomNode> indexes = new TreeMap<>(Comparator.comparing(e -> Integer.valueOf(e.substring(1))));
		if(list != null)
			for(final GedcomNode elem : list)
				indexes.put(elem.getID(), elem);
		return indexes;
	}

	static Map<Integer, String> reverseMap(final Map<String, GedcomNode> map){
		final Map<Integer, String> values;
		if(!map.isEmpty()){
			values = new HashMap<>(map.size());
			for(final Map.Entry<String, GedcomNode> elem : map.entrySet())
				values.put(elem.getValue().hashCode(), elem.getKey());
		}
		else
			values = new HashMap<>(0);
		return values;
	}

}
