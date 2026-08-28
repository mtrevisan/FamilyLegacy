package io.github.mtrevisan.familylegacy.v2.gedcom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class GEDCOMParser{

	public List<GEDCOMNode> parse(Reader reader) throws IOException{
		List<GEDCOMNode> roots = new ArrayList<>();
		Deque<GEDCOMNode> stack = new ArrayDeque<>();
		BufferedReader br = new BufferedReader(reader);
		String line;
		GEDCOMNode lastNode = null;

		while((line = br.readLine()) != null){
			// Rimuove il BOM UTF-8 se presente al primo livello
			if(!line.isEmpty() && line.charAt(0) == '\uFEFF'){
				line = line.substring(1);
			}
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
					rest = rest.substring(endXref + 1).trim();
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

			// Concatenazione CONC / CONT esatta senza inserire spazi arbitrari
			if("CONC".equals(tag) || "CONT".equals(tag)){
				if(lastNode != null){
					String current = lastNode.getValue() != null ? lastNode.getValue() : "";
					String appendVal = value != null ? value : "";
					if("CONC".equals(tag)){
						current += appendVal;
					}
					else{
						current += "\n" + appendVal;
					}
					lastNode.setValue(current);
				}
				continue;
			}

			GEDCOMNode node = new GEDCOMNode(level, tag, value);
			if(xref != null){
				node.setXrefId(xref);
			}

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
					roots.add(node);
				}
				stack.push(node);
			}
			lastNode = node;
		}
		return roots;
	}

}
