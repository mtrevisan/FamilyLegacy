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
import java.util.List;


public abstract class NoteContainer extends ExtensionContainer{

	private List<NoteRef> noteRefs;
	private List<Note> notes;


	/**
	 * @param gedcom	Gedcom
	 * @return	Inline notes as well as referenced notes
	 */
	public List<Note> getAllNotes(final Gedcom gedcom){
		final List<NoteRef> noteRefs = getNoteRefs();
		final List<Note> notes = new ArrayList<>(noteRefs.size());
		for(final NoteRef noteRef : noteRefs){
			final Note note = noteRef.getNote(gedcom);
			if(note != null)
				notes.add(note);
		}
		notes.addAll(getNotes());
		return notes;
	}

	List<NoteRef> getNoteRefs(){
		return (noteRefs != null? noteRefs: Collections.emptyList());
	}

	public void setNoteRefs(final List<NoteRef> noteRefs){
		this.noteRefs = noteRefs;
	}

	public void addNoteRef(final NoteRef noteRef){
		if(noteRefs == null)
			noteRefs = new ArrayList<>(1);

		noteRefs.add(noteRef);
	}

	List<Note> getNotes(){
		return (notes != null? notes: Collections.emptyList());
	}

	public void setNotes(final List<Note> notes){
		this.notes = notes;
	}

	public void addNote(final Note note){
		if(notes == null)
			notes = new ArrayList<>(1);

		notes.add(note);
	}

	//FIXME
//	public void visitContainedObjects(final Visitor visitor){
//		for(final NoteRef noteRef : getNoteRefs())
//			noteRef.accept(visitor);
//		for(final Note note : getNotes())
//			note.accept(visitor);
//
//		super.visitContainedObjects(visitor);
//	}

}
