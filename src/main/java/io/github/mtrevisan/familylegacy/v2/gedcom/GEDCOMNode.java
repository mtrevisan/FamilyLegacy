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
package io.github.mtrevisan.familylegacy.v2.gedcom;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a node in a GEDCOM tree.
 * Each node has a level, tag, optional value, optional cross‑reference ID, and a list of children.
 */
public class GEDCOMNode{

	private final int level;
	private final String tag;
	private String value;
	private String xrefId;    // e.g., @I123@, only for level 0 records
	private final List<GEDCOMNode> children = new ArrayList<>();


	public GEDCOMNode(int level, String tag, String value){
		this.level = level;
		this.tag = tag;
		this.value = value;
	}

	public int getLevel(){
		return level;
	}

	public String getTag(){
		return tag;
	}

	public String getValue(){
		return value;
	}

	public void setValue(String s){
		value = s;
	}

	public String getXrefId(){
		return xrefId;
	}

	public void setXrefId(String xrefId){
		this.xrefId = xrefId;
	}

	public List<GEDCOMNode> getChildren(){
		return children;
	}

	public void addChild(GEDCOMNode child){
		children.add(child);
	}

	@Override
	public String toString(){
		return "GEDCOMNode{" + "level=" + level + ", tag='" + tag + '\'' + ", value='" + value + '\'' +
			(xrefId != null? ", xrefId='" + xrefId + '\'': "") + ", children=" + children.size() + '}';
	}

}
