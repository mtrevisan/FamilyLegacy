package io.github.mtrevisan.familylegacy.v2.gedcom;


import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 * Parser for GEDCOM 5.5.1 files.
 * It reads the file line by line, splits into level, tag, and value,
 * handles cross‑references (@...@), and assembles a tree of GEDCOMNode objects.
 * CONC and CONT lines are concatenated into the value of the preceding node.
 */
public class GEDCOMParser{

	public List<GEDCOMNode> parse(Reader reader) throws IOException{
		List<GEDCOMNode> roots = new ArrayList<>();
		Deque<GEDCOMNode> stack = new ArrayDeque<>();
		BufferedReader br = new BufferedReader(reader);
		String line;
		GEDCOMNode lastNode = null;

		while((line = br.readLine()) != null){
			line = line.trim();
			if(line.isEmpty()) continue;

			// Parse level, tag, value
			int firstSpace = line.indexOf(' ');
			if(firstSpace == -1) continue;
			int level;
			try{
				level = Integer.parseInt(line.substring(0, firstSpace));
			}
			catch(NumberFormatException e){
				continue; // skip malformed
			}
			String rest = line.substring(firstSpace + 1).trim();

			// Check for cross‑reference tag (e.g., @I123@)
			String tag, value = null;
			String xref = null;
			if(rest.startsWith("@") && rest.indexOf('@', 1) > 1){
				int endXref = rest.indexOf('@', 1);
				xref = IDNormalizer.clean(rest.substring(0, endXref + 1));
				rest = rest.substring(endXref + 1).trim();
				int space = rest.indexOf(' ');
				if(space == -1){
					tag = rest;
					value = null;
				}
				else{
					tag = rest.substring(0, space);
					value = rest.substring(space + 1).trim();
				}
			}
			else{
				int space = rest.indexOf(' ');
				if(space == -1){
					tag = rest;
					value = null;
				}
				else{
					tag = rest.substring(0, space);
					value = rest.substring(space + 1).trim();
				}
			}

			// Handle CONC / CONT lines
			if(tag.equals("CONC") || tag.equals("CONT")){
				if(lastNode != null){
					String current = lastNode.getValue();
					if(current == null) current = "";
					if(tag.equals("CONC")){
						current += (value != null? value: "");
					}
					else{
						current += "\n" + (value != null? value: "");
					}
					lastNode.setValue(current);
				}
				continue; // do not create a node for CONC/CONT
			}

			GEDCOMNode node = new GEDCOMNode(level, tag, value);
			if(xref != null) node.setXrefId(xref);

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
					stack.peek().addChild(node);
				}
				else{
					roots.add(node); // fallback
				}
				stack.push(node);
			}
			lastNode = node;
		}
		return roots;
	}

}
