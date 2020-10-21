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
package io.github.mtrevisan.familylegacy.gedcom_old.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Gedcom{

	private Header head;
	private List<Submitter> subms;
	private Submission subn;
	private List<Person> people;
	private List<Family> families;
	private List<Media> media;
	private List<Note> notes;
	private List<Source> sources;
	private List<Repository> repositories;

	private Map<String, Person> personIndex;
	private Map<String, Family> familyIndex;
	private Map<String, Media> mediaIndex;
	private Map<String, Note> noteIndex;
	private Map<String, Source> sourceIndex;
	private Map<String, Repository> repositoryIndex;
	private Map<String, Submitter> submitterIndex;


	public Header getHeader(){
		return head;
	}

	public void setHeader(final Header head){
		this.head = head;
	}

	public List<Person> getPeople(){
		return (people != null? people: Collections.emptyList());
	}

	public void setPeople(final List<Person> people){
		this.people = people;
	}

	public Person getPerson(final String id){
		return personIndex.get(id);
	}

	public void addPerson(final Person person){
		if(people == null)
			people = new ArrayList<>(1);

		people.add(person);
		if(personIndex != null)
			personIndex.put(person.getId(), person);
	}

	public List<Family> getFamilies(){
		return (families != null? families: Collections.emptyList());
	}

	public Family getFamily(final String id){
		return familyIndex.get(id);
	}

	public void setFamilies(final List<Family> families){
		this.families = families;
	}

	public void addFamily(final Family family){
		if(families == null)
			families = new ArrayList<>(1);

		families.add(family);
		if(familyIndex != null)
			familyIndex.put(family.getId(), family);
	}

	public List<Media> getMedia(){
		return (media != null? media: Collections.emptyList());
	}

	public Media getMedia(final String id){
		return mediaIndex.get(id);
	}

	public void setMedia(final List<Media> media){
		this.media = media;
	}

	public void addMedia(final Media m){
		if(media == null)
			media = new ArrayList<>(1);

		media.add(m);
		if(mediaIndex != null)
			mediaIndex.put(m.getId(), m);
	}

	public List<Note> getNotes(){
		return (notes != null? notes: Collections.emptyList());
	}

	public Note getNote(final String id){
		return noteIndex.get(id);
	}

	public void setNotes(final List<Note> notes){
		this.notes = notes;
	}

	public void addNote(final Note note){
		if(notes == null)
			notes = new ArrayList<>(1);

		notes.add(note);
		if(noteIndex != null)
			noteIndex.put(note.getId(), note);
	}

	public List<Source> getSources(){
		return (sources != null? sources: Collections.emptyList());
	}

	public Source getSource(final String id){
		return sourceIndex.get(id);
	}

	public void setSources(final List<Source> sources){
		this.sources = sources;
	}

	public void addSource(final Source source){
		if(sources == null)
			sources = new ArrayList<>(1);

		sources.add(source);
		if(sourceIndex != null)
			sourceIndex.put(source.getId(), source);
	}

	public List<Repository> getRepositories(){
		return (repositories != null? repositories: Collections.emptyList());
	}

	public Repository getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public void setRepositories(final List<Repository> repositories){
		this.repositories = repositories;
	}

	public void addRepository(final Repository repository){
		if(repositories == null)
			repositories = new ArrayList<>(1);

		repositories.add(repository);
		if(repositoryIndex != null)
			repositoryIndex.put(repository.getId(), repository);
	}

	public Submitter getSubmitter(final String id){
		return submitterIndex.get(id);
	}

	public List<Submitter> getSubmitters(){
		return (subms != null? subms: Collections.emptyList());
	}

	public void setSubmitters(final List<Submitter> submitters){
		subms = submitters;
	}

	public void addSubmitter(final Submitter submitter){
		if(subms == null)
			subms = new ArrayList<>(1);

		subms.add(submitter);

		if(submitterIndex != null)
			submitterIndex.put(submitter.getId(), submitter);
	}

	public Submission getSubmission(){
		if(subn != null)
			return subn;
		if(head != null)
			return head.getSubmission();
		return null;
	}

	public void setSubmission(final Submission subn){
		this.subn = subn;
	}

	public void createIndexes(){
		personIndex = new HashMap<>(getPeople().size());
		for(final Person person : getPeople())
			personIndex.put(person.getId(), person);

		familyIndex = new HashMap<>(getFamilies().size());
		for(final Family family : getFamilies())
			familyIndex.put(family.getId(), family);

		mediaIndex = new HashMap<>(getMedia().size());
		for(final Media m : getMedia())
			mediaIndex.put(m.getId(), m);

		noteIndex = new HashMap<>(getNotes().size());
		for(final Note note : getNotes())
			noteIndex.put(note.getId(), note);

		sourceIndex = new HashMap<>(getSources().size());
		for(final Source source : getSources())
			sourceIndex.put(source.getId(), source);

		repositoryIndex = new HashMap<>(getRepositories().size());
		for(final Repository repository : getRepositories())
			repositoryIndex.put(repository.getId(), repository);

		submitterIndex = new HashMap<>(getSubmitters().size());
		for(final Submitter submitter : getSubmitters())
			submitterIndex.put(submitter.getId(), submitter);
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(head != null)
//				head.accept(visitor);
//			for(Submitter submitter : getSubmitters())
//				submitter.accept(visitor);
//			if(subn != null)
//				subn.accept(visitor);
//			for(Person person : getPeople())
//				person.accept(visitor);
//			for(Family family : getFamilies())
//				family.accept(visitor);
//			for(Media media : getMedia())
//				media.accept(visitor);
//			for(Note note : getNotes())
//				note.accept(visitor);
//			for(Source source : getSources())
//				source.accept(visitor);
//			for(Repository repository : getRepositories())
//				repository.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
