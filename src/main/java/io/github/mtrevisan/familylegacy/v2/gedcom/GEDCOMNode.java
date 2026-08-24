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
