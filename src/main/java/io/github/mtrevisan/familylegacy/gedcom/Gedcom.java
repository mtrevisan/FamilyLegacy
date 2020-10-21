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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Gedcom{

	private static final String LINE_SEPARATOR = "\r\n";
	private static final String INDENTATION = "  ";


	private GedcomNode root;

	private GedcomNode head;
	private List<GedcomNode> submitters;
	private GedcomNode submission;
	private List<GedcomNode> people;
	private List<GedcomNode> families;
	private List<GedcomNode> media;
	private List<GedcomNode> notes;
	private List<GedcomNode> sources;
	private List<GedcomNode> repositories;

	private Map<String, GedcomNode> personIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> mediaIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> submitterIndex;


	public static void main(final String[] args){
		try{
//			final Gedcom gedcom = load("/gedg/gedcomobjects_5.5.1.gedg", "/ged/Case001-AddressStructure.ged");
//			final Gedcom gedcom = load("/gedg/gedcomobjects_5.5.1.gedg", "/ged/complex.ged");
			final Gedcom gedcom = load("/gedg/gedcomobjects_5.5.gedg", "/ged/complex.ged");

//			final StringBuilder sb = gedcom.printWithIndentation();
			final StringBuilder sb = gedcom.printFlat();
System.out.println(gedcom);
		}
		catch(final GedcomGrammarParseException | GedcomParseException e){
			e.printStackTrace();
		}
	}

	public static Gedcom load(final String grammarFile, final String gedcomFile) throws GedcomGrammarParseException, GedcomParseException{
		final GedcomGrammar grammar = GedcomGrammar.create(grammarFile);

		final GedcomNode root = GedcomParser.parse(gedcomFile, grammar);

		return create(root);
	}

	private static Gedcom create(final GedcomNode root) throws GedcomParseException{
		final Gedcom g = new Gedcom();
		g.root = root;
		final List<GedcomNode> heads = root.getChildrenWithTag("HEAD");
		if(heads.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		g.head = heads.get(0);
		g.people = root.getChildrenWithTag("INDI");
		g.families = root.getChildrenWithTag("FAM");
		g.media = root.getChildrenWithTag("OBJE");
		g.notes = root.getChildrenWithTag("NOTE");
		g.repositories = root.getChildrenWithTag("REPO");
		g.sources = root.getChildrenWithTag("SOUR");
		List<GedcomNode> submissions = root.getChildrenWithTag("SUBN");
		if(submissions.isEmpty())
			submissions = g.head.getChildrenWithTag("SUBN");
		if(submissions.size() > 1)
			throw GedcomParseException.create("Required submission tag missing");
		if(!submissions.isEmpty())
			g.submission = submissions.get(0);
		g.submitters = root.getChildrenWithTag("SUBM");

		g.personIndex = generateIndexes(g.people);
		g.familyIndex = generateIndexes(g.families);
		g.mediaIndex = generateIndexes(g.media);
		g.noteIndex = generateIndexes(g.notes);
		g.sourceIndex = generateIndexes(g.sources);
		g.repositoryIndex = generateIndexes(g.repositories);
		g.submitterIndex = generateIndexes(g.submitters);

		return g;
	}

	/**
	 * Prints the GEDCOM file using indentation.
	 */
	public StringBuilder printWithIndentation(){
		return print(true);
	}

	/**
	 * Prints the GEDCOM file without indentation.
	 */
	public StringBuilder printFlat(){
		return print(false);
	}

	public StringBuilder print(final boolean indent){
		final StringBuilder sb = new StringBuilder();
		final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
		//skip passed node and add its children
		for(final GedcomNode child : root.getChildren())
			nodeStack.addLast(child);
		while(!nodeStack.isEmpty()){
			final GedcomNode child = nodeStack.pop();
			final List<GedcomNode> children = child.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				nodeStack.addFirst(children.get(i));

			if(indent)
				sb.append(StringUtils.repeat(INDENTATION, child.getLevel()));
			sb.append(child.getLevel());
			if(child.getLevel() == 0){
				appendID(sb, child.getID());
				appendTag(sb, child.getTag());
			}
			else{
				appendTag(sb, child.getTag());
				appendID(sb, child.getXRef());
				appendID(sb, child.getID());
			}
			if(child.getValue() != null)
				sb.append(' ').append(child.getValue());
			sb.append(LINE_SEPARATOR);
		}

		return sb;
	}

	private void appendID(final StringBuilder sb, final String id){
		if(id != null)
			sb.append(' ').append('@').append(id).append('@');
	}

	private void appendTag(final StringBuilder sb, final String tag){
		sb.append(' ').append(tag);
	}

	private static Map<String, GedcomNode> generateIndexes(final Collection<GedcomNode> list){
		final Map<String, GedcomNode> indexes;
		if(!list.isEmpty()){
			indexes = new HashMap<>(list.size());
			for(final GedcomNode elem : list)
				indexes.put(elem.getID(), elem);
		}
		else
			indexes = Collections.emptyMap();
		return indexes;
	}

	public GedcomNode getHeader(){
		return head;
	}

	public List<GedcomNode> getSubmitters(){
		return submitters;
	}

	public GedcomNode getSubmitter(final String id){
		return submitterIndex.get(id);
	}

	public GedcomNode getSubmission(){
		return submission;
	}

	public List<GedcomNode> getPeople(){
		return people;
	}

	public GedcomNode getPerson(final String id){
		return personIndex.get(id);
	}

	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public List<GedcomNode> getMedia(){
		return media;
	}

	public GedcomNode getMedia(final String id){
		return mediaIndex.get(id);
	}

	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

}
