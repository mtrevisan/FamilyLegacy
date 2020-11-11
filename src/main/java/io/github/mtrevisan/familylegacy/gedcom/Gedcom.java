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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Gedcom extends Store<Gedcom>{

	private static final String TAG_HEADER = "HEAD";
	private static final String TAG_INDIVIDUAL = "INDI";
	private static final String TAG_FAMILY = "FAM";
	private static final String TAG_DOCUMENT = "OBJE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPO";
	private static final String TAG_SOURCE = "SOUR";
	private static final String TAG_SUBMITTER = "SUBM";
	private static final String TAG_SUBMISSION = "SUBN";
	private static final String TAG_CHARSET = "CHAR";


	private GedcomNode header = GedcomNode.createEmpty();
	private List<GedcomNode> individuals;
	private List<GedcomNode> families;
	private List<GedcomNode> documents;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> submitters;
	private GedcomNode submission;

	private Map<String, GedcomNode> individualIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> documentIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> submitterIndex;


	public static void main(final String[] args){
		try{
			final Gedcom store = new Gedcom();
//			final Gedcom gedcom = store.load("/gedg/gedcomobjects_5.5.gedg", "/ged/large.ged");
			final Gedcom gedcom = store.load("/gedg/gedcomobjects_5.5.1.gedg", "/ged/small.ged");
//			final Gedcom gedcom = store.load("/gedg/gedcomobjects_5.5.1.tcgb.gedg", "/ged/large.ged");

			gedcom.transform();

			final OutputStream os = new FileOutputStream(new File("./tmp.ged"));
			gedcom.write(os);
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected Gedcom create(final GedcomNode root) throws GedcomParseException{
		final Gedcom g = new Gedcom();
		g.root = root;
		final List<GedcomNode> heads = root.getChildrenWithTag(TAG_HEADER);
		if(heads.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		g.header = heads.get(0);
		g.individuals = root.getChildrenWithTag(TAG_INDIVIDUAL);
		g.families = root.getChildrenWithTag(TAG_FAMILY);
		g.documents = root.getChildrenWithTag(TAG_DOCUMENT);
		g.notes = root.getChildrenWithTag(TAG_NOTE);
		g.repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		g.sources = root.getChildrenWithTag(TAG_SOURCE);
		g.submitters = root.getChildrenWithTag(TAG_SUBMITTER);
		List<GedcomNode> submissions = root.getChildrenWithTag(TAG_SUBMISSION);
		if(submissions.isEmpty())
			submissions = g.header.getChildrenWithTag(TAG_SUBMISSION);
		if(submissions.size() > 1)
			throw GedcomParseException.create("Required submission tag missing");
		if(!submissions.isEmpty())
			g.submission = submissions.get(0);

		g.individualIndex = generateIndexes(g.individuals);
		g.familyIndex = generateIndexes(g.families);
		g.documentIndex = generateIndexes(g.documents);
		g.noteIndex = generateIndexes(g.notes);
		g.repositoryIndex = generateIndexes(g.repositories);
		g.sourceIndex = generateIndexes(g.sources);
		g.submitterIndex = generateIndexes(g.submitters);

		return g;
	}

	@Override
	protected String getCharsetName(){
		final List<GedcomNode> source = header.getChildrenWithTag(TAG_SOURCE);
		final String generator = (!source.isEmpty()? source.get(0).getValue(): null);
		final List<GedcomNode> characterSet = header.getChildrenWithTag(TAG_CHARSET);
		String charset = (!characterSet.isEmpty()? characterSet.get(0).getValue(): null);
		final List<GedcomNode> characterSetVersion = (!characterSet.isEmpty()? characterSet.get(0).getChildren(): Collections.emptyList());
		final String version = (!characterSetVersion.isEmpty()? characterSetVersion.get(0).getValue(): null);
		charset = GedcomHelper.getCorrectedCharsetName(generator, charset, version);
		if(charset.isEmpty())
			//default
			charset = StandardCharsets.UTF_8.name();
		return charset;
	}

	public GedcomNode getHeader(){
		return header;
	}

	public List<GedcomNode> getIndividuals(){
		return individuals;
	}

	public GedcomNode getIndividual(final String id){
		return individualIndex.get(id);
	}

	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public List<GedcomNode> getDocuments(){
		return documents;
	}

	public GedcomNode getDocument(final String id){
		return documentIndex.get(id);
	}

	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public void addNote(final GedcomNode note){
		if(notes == null){
			notes = new ArrayList<>(1);
			noteIndex = new HashMap<>(1);
		}
		notes.add(note);
		noteIndex.put(note.getID(), note);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public void addRepository(final GedcomNode repository){
		if(repositories == null){
			repositories = new ArrayList<>(1);
			repositoryIndex = new HashMap<>(1);
		}
		repositories.add(repository);
		repositoryIndex.put(repository.getID(), repository);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public void addSource(final GedcomNode source){
		if(sources == null){
			sources = new ArrayList<>(1);
			sourceIndex = new HashMap<>(1);
		}
		sources.add(source);
		sourceIndex.put(source.getID(), source);
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

}
