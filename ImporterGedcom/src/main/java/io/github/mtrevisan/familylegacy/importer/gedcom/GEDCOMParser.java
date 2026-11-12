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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 * Reads a GEDCOM text into a forest of nodes.
 * <p>
 * The pointers are left as written: the xrefs are given their final form by {@link XrefNormalizer}.
 */
public class GEDCOMParser{

	public List<GEDCOMNode> parse(final Reader reader) throws IOException{
		final List<GEDCOMNode> roots = new ArrayList<>();
		final Deque<GEDCOMNode> stack = new ArrayDeque<>();
		final BufferedReader br = (reader instanceof BufferedReader buffered? buffered: new BufferedReader(reader));
		String line;
		boolean gedcom7 = false;

		while((line = br.readLine()) != null){
			// Rimuove il BOM UTF-8 se presente al primo livello
			if(!line.isEmpty() && line.charAt(0) == '﻿'){
				line = line.substring(1);
			}
			// a line can be indented
			line = StringUtils.stripStart(line, null);
			if(line.isEmpty()){
				continue;
			}

			int firstSpace = line.indexOf(' ');
			if(firstSpace == -1){
				continue;
			}

			int level;
			try{
				level = Integer.parseInt(line.substring(0, firstSpace));
			}
			catch(NumberFormatException e){
				continue;
			}

			String rest = line.substring(firstSpace + 1);
			String tag;
			String value = null;
			String xref = null;

			if(rest.startsWith("@")){
				int endXref = rest.indexOf('@', 1);
				if(endXref > 1){
					xref = GEDCOMHelper.cleanId(rest.substring(0, endXref + 1));
					rest = rest.substring(endXref + 1)
						.trim();
				}
			}

			int space = rest.indexOf(' ');
			if(space == -1){
				tag = rest;
			}
			else{
				tag = rest.substring(0, space);
				value = rest.substring(space + 1); // Preserva gli spazi iniziali del valore
			}

			// '@' inside a text value is escaped as '@@' (pointers never contain it); GEDCOM 7 doubles only a leading '@'
			if(value != null)
				value = (gedcom7
					? (value.startsWith("@@")? value.substring(1): value)
					: Strings.CS.replace(value, "@@", "@"));

			// Concatenazione CONC / CONT esatta senza inserire spazi arbitrari
			if("CONC".equals(tag) || "CONT".equals(tag)){
				// CONC/CONT continue the value of their superior (level - 1), not of the last line read, which can be a
				// sibling sub-structure
				while(!stack.isEmpty() && stack.peek().getLevel() >= level)
					stack.pop();
				final GEDCOMNode target = stack.peek();
				if(target != null){
					String current = (target.getValue() != null? target.getValue(): StringUtils.EMPTY);
					String appendVal = (value != null? value: StringUtils.EMPTY);
					if("CONC".equals(tag))
						current += appendVal;
					else
						current += StringUtils.LF + appendVal;
					target.setValue(current);
				}
				continue;
			}

			GEDCOMNode node = new GEDCOMNode(level, tag, value);
			if(xref != null)
				node.setXrefId(xref);

			if(level == 0){
				roots.add(node);
				stack.clear();
				stack.push(node);
			}
			else{
				while(!stack.isEmpty() && stack.peek().getLevel() >= level){
					stack.pop();
				}
				if(!stack.isEmpty()){
					// HEAD.GEDC.VERS, the version, comes before any record
					if(level == 2 && GEDCOMMapper.TAG_VERSION.equals(tag) && "GEDC".equals(stack.peek().getTag()) && value != null
							&& value.trim().startsWith("7"))
						gedcom7 = true;
					stack.peek()
						.addChild(node);
				}
				else{
					roots.add(node);
				}
				stack.push(node);
			}
		}
		return roots;
	}

}
