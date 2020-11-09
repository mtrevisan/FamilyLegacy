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


public class Flef extends Store<Flef>{

	private static final String ID_INDIVIDUAL_PREFIX = "I";
	private static final String ID_FAMILY_PREFIX = "F";
	private static final String ID_PLACE_PREFIX = "P";
	private static final String ID_DOCUMENT_PREFIX = "D";
	private static final String ID_NOTE_PREFIX = "N";
	private static final String ID_REPOSITORY_PREFIX = "R";
	private static final String ID_SOURCE_PREFIX = "S";
	private static final String ID_SUBMITTER_PREFIX = "M";

	private static final String TAG_HEADER = "HEADER";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_FAMILY = "FAMILY";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_CHARSET = "CHARSET";


	private GedcomNode header = GedcomNode.createEmpty();
	private List<GedcomNode> people;
	private List<GedcomNode> families;
	private List<GedcomNode> places;
	private List<GedcomNode> documents;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> submitters;

	private Map<String, GedcomNode> personIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> placeIndex;
	private Map<String, GedcomNode> documentIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> submitterIndex;


	public static void main(final String[] args){
		try{
			final Flef store = new Flef();
			final Flef flef = store.load("/gedg/flef_0.0.1.gedg", "/ged/small.flef.ged");

			flef.transform();

			final OutputStream os = new FileOutputStream(new File("./tmp.ged"));
			flef.write(os);
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected Flef create(final GedcomNode root) throws GedcomParseException{
		final Flef g = new Flef();
		g.root = root;
		final List<GedcomNode> headers = root.getChildrenWithTag(TAG_HEADER);
		if(headers.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		g.header = headers.get(0);
		g.people = root.getChildrenWithTag(TAG_INDIVIDUAL);
		g.families = root.getChildrenWithTag(TAG_FAMILY);
		g.places = root.getChildrenWithTag(TAG_PLACE);
		g.documents = root.getChildrenWithTag(TAG_DOCUMENT);
		g.notes = root.getChildrenWithTag(TAG_NOTE);
		g.repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		g.sources = root.getChildrenWithTag(TAG_SOURCE);
		g.submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		g.personIndex = generateIndexes(g.people);
		g.familyIndex = generateIndexes(g.families);
		g.placeIndex = generateIndexes(g.places);
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

	public List<GedcomNode> getPeople(){
		return people;
	}

	public GedcomNode getPerson(final String id){
		return personIndex.get(id);
	}

	public GedcomNode addPerson(final GedcomNode person){
		if(people == null){
			people = new ArrayList<>(1);
			personIndex = new HashMap<>(1);
		}
		person.withID(getNextPersonID(people.size()));
		people.add(person);
		personIndex.put(person.getID(), person);
		return person;
	}

	public static String getNextPersonID(final int peopleCount){
		return ID_INDIVIDUAL_PREFIX + (peopleCount + 1);
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
		family.withID(getNextFamilyID(families.size()));
		families.add(family);
		familyIndex.put(family.getID(), family);
		return family;
	}

	public static String getNextFamilyID(final int familiesCount){
		return ID_FAMILY_PREFIX + (familiesCount + 1);
	}

	public List<GedcomNode> getPlaces(){
		return (places != null? places: Collections.emptyList());
	}

	public GedcomNode getPlace(final String id){
		return placeIndex.get(id);
	}

	public GedcomNode addPlace(final GedcomNode place){
		if(places == null){
			places = new ArrayList<>(1);
			placeIndex = new HashMap<>(1);
		}
		place.withID(getNextPlaceID(places.size()));
		places.add(place);
		placeIndex.put(place.getID(), place);
		return place;
	}

	public static String getNextPlaceID(final int placesCount){
		return ID_PLACE_PREFIX + (placesCount + 1);
	}

	public List<GedcomNode> getDocuments(){
		return documents;
	}

	public GedcomNode getDocument(final String id){
		return documentIndex.get(id);
	}

	public GedcomNode addDocument(final GedcomNode document){
		if(documents == null){
			documents = new ArrayList<>(1);
			documentIndex = new HashMap<>(1);
		}
		document.withID(getNextDocumentID(documents.size()));
		documents.add(document);
		documentIndex.put(document.getID(), document);
		return document;
	}

	public static String getNextDocumentID(final int documentCount){
		return ID_DOCUMENT_PREFIX + (documentCount + 1);
	}

	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public GedcomNode addNote(final GedcomNode note){
		if(notes == null){
			notes = new ArrayList<>(1);
			noteIndex = new HashMap<>(1);
		}
		note.withID(getNextNoteID(notes.size()));
		notes.add(note);
		noteIndex.put(note.getID(), note);
		return note;
	}

	public static String getNextNoteID(final int noteCount){
		return ID_NOTE_PREFIX + (noteCount + 1);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public GedcomNode addRepository(final GedcomNode repository){
		if(repositories == null){
			repositories = new ArrayList<>(1);
			repositoryIndex = new HashMap<>(1);
		}
		repository.withID(getNextRepositoryID(repositories.size()));
		repositories.add(repository);
		repositoryIndex.put(repository.getID(), repository);
		return repository;
	}

	public static String getNextRepositoryID(final int repositoryCount){
		return ID_REPOSITORY_PREFIX + (repositoryCount + 1);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public GedcomNode addSource(final GedcomNode source){
		if(sources == null){
			sources = new ArrayList<>(1);
			sourceIndex = new HashMap<>(1);
		}
		source.withID(getNextSourceID(sources.size()));
		sources.add(source);
		sourceIndex.put(source.getID(), source);
		return source;
	}

	public static String getNextSourceID(final int sourceCount){
		return ID_SOURCE_PREFIX + (sourceCount + 1);
	}

	public List<GedcomNode> getSubmitters(){
		return submitters;
	}

	public GedcomNode getSubmitter(final String id){
		return submitterIndex.get(id);
	}

	public GedcomNode addSubmitter(final GedcomNode submitter){
		if(submitters == null){
			submitters = new ArrayList<>(1);
			submitterIndex = new HashMap<>(1);
		}
		submitter.withID(getNextSubmitterID(submitters.size()));
		submitters.add(submitter);
		submitterIndex.put(submitter.getID(), submitter);
		return submitter;
	}

	public static String getNextSubmitterID(final int submitterCount){
		return ID_SUBMITTER_PREFIX + (submitterCount + 1);
	}

}
