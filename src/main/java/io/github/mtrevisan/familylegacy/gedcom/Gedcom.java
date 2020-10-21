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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

			OutputStream os = new FileOutputStream(new File("/tmp.ged"));
//			gedcom.writeWithIndentation(os);
			gedcom.writeFlat(os);
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
	public void writeFlat(final OutputStream os) throws IOException{
		writeWithIndentation(os, 0);
	}

	/**
	 * Prints the GEDCOM file using indentation.
	 */
	public void writeWithIndentation(final OutputStream os, final int spaces) throws IOException{
		write(os, StringUtils.repeat(StringUtils.SPACE, spaces));
	}

	private void write(final OutputStream os, final String indentation) throws IOException{
		final String charset = getCharsetName();
		final String eol = (charset.equals("x-MacRoman")? "\r": "\n");
		final OutputStreamWriter writer = (AnselInputStreamReader.CHARACTER_ENCODING.equals(charset)?
			new AnselOutputStreamWriter(os): new OutputStreamWriter(os, charset));
		final Writer out = new BufferedWriter(writer);

		final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
		//skip passed node and add its children
		for(final GedcomNode child : root.getChildren())
			nodeStack.addLast(child);
		while(!nodeStack.isEmpty()){
			final GedcomNode child = nodeStack.pop();
			final List<GedcomNode> children = child.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				nodeStack.addFirst(children.get(i));

			out.write(indentation);
			out.write(child.getLevel());
			if(child.getLevel() == 0){
				appendID(out, child.getID());
				appendTag(out, child.getTag());
			}
			else{
				appendTag(out, child.getTag());
				appendID(out, child.getXRef());
				appendID(out, child.getID());
			}
			if(child.getValue() != null){
				out.write(' ');
				out.write(child.getValue());
			}
			out.write(eol);
		}
		out.flush();
	}

	private String getCharsetName(){
		final GedcomNode header = getHeader();
		final List<GedcomNode> source = header.getChildrenWithTag("SOUR");
		final String generator = (!source.isEmpty()? source.get(0).getValue(): null);
		final List<GedcomNode> characterSet = header.getChildrenWithTag("CHAR");
		String charset = (!characterSet.isEmpty()? characterSet.get(0).getValue(): null);
		final List<GedcomNode> characterSetVersion = (!characterSet.isEmpty()? characterSet.get(0).getChildren(): null);
		final String version = (!characterSetVersion.isEmpty()? characterSetVersion.get(0).getValue(): null);
		charset = GedcomHelper.getCorrectedCharsetName(generator, charset, version);
		if(charset.isEmpty())
			//default
			charset = StandardCharsets.UTF_8.name();
		return charset;
	}

	private void appendID(final Writer out, final String id) throws IOException{
		if(id != null){
			out.write(' ');
			out.write('@');
			out.write(id);
			out.write('@');
		}
	}

	private void appendTag(final Writer out, final String tag) throws IOException{
		out.write(' ');
		out.write(tag);
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
