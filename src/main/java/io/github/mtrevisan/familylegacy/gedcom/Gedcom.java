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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Gedcom{

	private static final byte[] LINE_SEPARATOR = "\r\n".getBytes();


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
			final Gedcom gedcom = load("/gedg/gedcomobjects_5.5.1.gedg", "/ged/complex.ged");
//			final Gedcom gedcom = load("/gedg/gedcomobjects_5.5.gedg", "/ged/complex.ged");

			OutputStream os = new FileOutputStream();
//			gedcom.printWithIndentation(os);
			gedcom.printFlat(os, StandardCharsets.UTF_8);
System.out.println(os.toString());
		}
		catch(final GedcomGrammarParseException | GedcomParseException | IOException e){
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
	 * Prints the GEDCOM file without indentation.
	 */
	public void printFlat(final OutputStream os, final Charset charset) throws IOException{
		printWithIndentation(os, 0, charset);
	}

	/**
	 * Prints the GEDCOM file using indentation.
	 */
	public void printWithIndentation(final OutputStream os, final int spaces, final Charset charset) throws IOException{
		print(os, StringUtils.repeat(StringUtils.SPACE, spaces).getBytes(), charset);
	}

	private void print(final OutputStream os, final byte[] indentation, final Charset charset) throws IOException{
		final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
		//skip passed node and add its children
		for(final GedcomNode child : root.getChildren())
			nodeStack.addLast(child);
		while(!nodeStack.isEmpty()){
			final GedcomNode child = nodeStack.pop();
			final List<GedcomNode> children = child.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				nodeStack.addFirst(children.get(i));

			os.write(indentation);
			os.write(Integer.toString(child.getLevel()).getBytes());
			if(child.getLevel() == 0){
				appendID(os, child.getID(), charset);
				appendTag(os, child.getTag(), charset);
			}
			else{
				appendTag(os, child.getTag(), charset);
				appendID(os, child.getXRef(), charset);
				appendID(os, child.getID(), charset);
			}
			if(child.getValue() != null){
				os.write(' ');
				os.write(child.getValue().getBytes(charset));
			}
			os.write(LINE_SEPARATOR);
		}
		os.flush();
	}

	private void appendID(final OutputStream os, final String id, final Charset charset) throws IOException{
		if(id != null){
			os.write(' ');
			os.write('@');
			os.write(id.getBytes(charset));
			os.write('@');
		}
	}

	private void appendTag(final OutputStream os, final String tag, final Charset charset) throws IOException{
		os.write(' ');
		os.write(tag.getBytes(charset));
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
