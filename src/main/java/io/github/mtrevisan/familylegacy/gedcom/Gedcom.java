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

import io.github.mtrevisan.familylegacy.gedcom.transformations.DocumentTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.FamilyTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.HeaderTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.IndividualTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.NoteTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.RepositoryTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SourceTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SubmitterTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Gedcom extends Store{

	private static final String TAG_HEADER = "HEAD";
	private static final String TAG_INDIVIDUAL = "INDI";
	private static final String TAG_FAMILY = "FAM";
	private static final String TAG_DOCUMENT = "OBJE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPO";
	private static final String TAG_SOURCE = "SOUR";
	private static final String TAG_SUBMITTER = "SUBM";
	private static final String TAG_CHARSET = "CHAR";

	private static final Transformation<Gedcom, Flef> HEADER_TRANSFORMATION = new HeaderTransformation();
	private static final Transformation<Gedcom, Flef> INDIVIDUAL_TRANSFORMATION = new IndividualTransformation();
	private static final Transformation<Gedcom, Flef> FAMILY_TRANSFORMATION = new FamilyTransformation();
	private static final Transformation<Gedcom, Flef> DOCUMENT_TRANSFORMATION = new DocumentTransformation();
	private static final Transformation<Gedcom, Flef> NOTE_TRANSFORMATION = new NoteTransformation();
	private static final Transformation<Gedcom, Flef> REPOSITORY_TRANSFORMATION = new RepositoryTransformation();
	private static final Transformation<Gedcom, Flef> SOURCE_TRANSFORMATION = new SourceTransformation();
	private static final Transformation<Gedcom, Flef> SUBMITTER_TRANSFORMATION = new SubmitterTransformation();


	private GedcomNode header;
	private List<GedcomNode> individuals;
	private List<GedcomNode> families;
	private List<GedcomNode> documents;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> submitters;

	private Map<String, GedcomNode> individualIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> documentIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> submitterIndex;

	private Map<GedcomNode, String> individualValue;
	private Map<GedcomNode, String> familyValue;
	private Map<GedcomNode, String> documentValue;
	private Map<GedcomNode, String> noteValue;
	private Map<GedcomNode, String> repositoryValue;
	private Map<GedcomNode, String> sourceValue;
	private Map<GedcomNode, String> submitterValue;


	public static void main(final String[] args){
		try{
			final Store storeGedcom = new Gedcom();
//			storeGedcom.load("/gedg/gedcomobjects_5.5.gedg", "src/main/resources/ged/large.ged");
//			storeGedcom.load("/gedg/gedcomobjects_5.5.1.gedg", "src/main/resources/ged/small.ged");
			storeGedcom.load("/gedg/gedcomobjects_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");

			final Flef storeFlef = storeGedcom.transform();

			final File outputFile = new File("tmp.ged");
			final OutputStream os = new FileOutputStream(outputFile);
//			gedcom.write(os);
			storeFlef.write(os);
			os.close();

			final Store storeFlef2 = new Flef();
			storeFlef2.load("/gedg/flef_0.0.2.gedg", outputFile.getPath());

			//compare storeFlef and storeFlef2
			final File outputFile2 = new File("tmp2.ged");
			final OutputStream os2 = new FileOutputStream(outputFile2);
			storeFlef.write(os2);
			os2.close();
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void create(final GedcomNode root) throws GedcomParseException{
		this.root = root;
		final List<GedcomNode> heads = root.getChildrenWithTag(TAG_HEADER);
		if(heads.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		header = heads.get(0);
		individuals = root.getChildrenWithTag(TAG_INDIVIDUAL);
		families = root.getChildrenWithTag(TAG_FAMILY);
		documents = root.getChildrenWithTag(TAG_DOCUMENT);
		notes = root.getChildrenWithTag(TAG_NOTE);
		repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		sources = root.getChildrenWithTag(TAG_SOURCE);
		submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		individualIndex = generateIndexes(individuals);
		familyIndex = generateIndexes(families);
		documentIndex = generateIndexes(documents);
		noteIndex = generateIndexes(notes);
		repositoryIndex = generateIndexes(repositories);
		sourceIndex = generateIndexes(sources);
		submitterIndex = generateIndexes(submitters);

		individualValue = reverseMap(individualIndex);
		familyValue = reverseMap(familyIndex);
		documentValue = reverseMap(documentIndex);
		noteValue = reverseMap(noteIndex);
		repositoryValue = reverseMap(repositoryIndex);
		sourceValue = reverseMap(sourceIndex);
		submitterValue = reverseMap(submitterIndex);
	}

	@Override
	public Flef transform(){
		final Flef destination = new Flef();
		HEADER_TRANSFORMATION.to(this, destination);
		INDIVIDUAL_TRANSFORMATION.to(this, destination);
		FAMILY_TRANSFORMATION.to(this, destination);
		DOCUMENT_TRANSFORMATION.to(this, destination);
		NOTE_TRANSFORMATION.to(this, destination);
		REPOSITORY_TRANSFORMATION.to(this, destination);
		SOURCE_TRANSFORMATION.to(this, destination);
		SUBMITTER_TRANSFORMATION.to(this, destination);
		return destination;
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

	@Override
	public void write(final OutputStream os) throws IOException{
		if(root == null)
			root = GedcomNodeBuilder.createRoot(Protocol.GEDCOM)
				.addChild(header)
				.addChildren(individuals)
				.addChildren(families)
				.addChildren(notes)
				.addChildren(repositories)
				.addChildren(sources)
				.addChildren(submitters)
				.addClosingChild("TRLR");

		super.write(os);
	}

	public GedcomNode getHeader(){
		return header;
	}

	public void setHeader(final GedcomNode header){
		this.header = header;
	}

	public List<GedcomNode> getIndividuals(){
		return individuals;
	}

	public GedcomNode getIndividual(final String id){
		return individualIndex.get(id);
	}

	public GedcomNode addIndividual(final GedcomNode individual){
		if(individuals == null){
			individuals = new ArrayList<>(1);
			individualIndex = new HashMap<>(1);
		}
		individuals.add(individual);
		individualIndex.put(individual.getID(), individual);
		return individual;
	}

	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public GedcomNode addFamily(final GedcomNode family){
		if(families == null){
			families = new ArrayList<>(1);
			familyIndex = new HashMap<>(1);
		}
		families.add(family);
		familyIndex.put(family.getID(), family);
		return family;
	}

	public List<GedcomNode> getDocuments(){
		return documents;
	}

	public GedcomNode getDocument(final String id){
		return documentIndex.get(id);
	}

	public void addDocument(final GedcomNode document){
		if(documents == null){
			documents = new ArrayList<>(1);
			documentIndex = new HashMap<>(1);
		}
		documents.add(document);
		documentIndex.put(document.getID(), document);
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

	public void addSubmitter(final GedcomNode submitter){
		if(submitters == null){
			submitters = new ArrayList<>(1);
			submitterIndex = new HashMap<>(1);
		}
		submitters.add(submitter);
		submitterIndex.put(submitter.getID(), submitter);
	}

}
