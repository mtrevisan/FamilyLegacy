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
package io.github.mtrevisan.familylegacy.gedcom_old;

import io.github.mtrevisan.familylegacy.gedcom_old.models.Address;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Association;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Change;
import io.github.mtrevisan.familylegacy.gedcom_old.models.CharacterSet;
import io.github.mtrevisan.familylegacy.gedcom_old.models.DateTime;
import io.github.mtrevisan.familylegacy.gedcom_old.models.EventFact;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Family;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom_old.models.GedcomVersion;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Generator;
import io.github.mtrevisan.familylegacy.gedcom_old.models.GeneratorCorporation;
import io.github.mtrevisan.familylegacy.gedcom_old.models.GeneratorData;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Header;
import io.github.mtrevisan.familylegacy.gedcom_old.models.LdsOrdinance;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Media;
import io.github.mtrevisan.familylegacy.gedcom_old.models.MediaContainer;
import io.github.mtrevisan.familylegacy.gedcom_old.models.MediaRef;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Name;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Note;
import io.github.mtrevisan.familylegacy.gedcom_old.models.NoteContainer;
import io.github.mtrevisan.familylegacy.gedcom_old.models.NoteRef;
import io.github.mtrevisan.familylegacy.gedcom_old.models.ParentFamilyRef;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Person;
import io.github.mtrevisan.familylegacy.gedcom_old.models.PersonFamilyCommonContainer;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Repository;
import io.github.mtrevisan.familylegacy.gedcom_old.models.RepositoryRef;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Source;
import io.github.mtrevisan.familylegacy.gedcom_old.models.SourceCitation;
import io.github.mtrevisan.familylegacy.gedcom_old.models.SourceCitationContainer;
import io.github.mtrevisan.familylegacy.gedcom_old.models.SpouseRef;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Submission;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Submitter;

import java.util.function.Function;


public enum GedcomTag{

	ABBR(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "Abbreviation"): null);
	}),
	ADDR(node -> {
		final Object parent = node.getParent().getObject();
		Address obj = null;
		if(parent instanceof GeneratorCorporation){
			obj = new Address();
			((GeneratorCorporation)parent).setAddress(obj);
		}
		else if(parent instanceof EventFact){
			obj = new Address();
			((EventFact)parent).setAddress(obj);
		}
		else if(parent instanceof Person){
			obj = new Address();
			((Person)parent).setAddress(obj);
		}
		else if(parent instanceof Repository){
			obj = new Address();
			((Repository)parent).setAddress(obj);
		}
		else if(parent instanceof Submitter){
			obj = new Address();
			((Submitter)parent).setAddress(obj);
		}
		return obj;
	}),
	ADOP,
	ADR1(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "AddressLine1"): null);
	}),
	ADR2(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "AddressLine2"): null);
	}),
	ADR3(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "AddressLine1"): null);
	}),
	AGE, AGNC, _AKA, ALIA, ANCI, ASSO,
	AUTH(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "Author"): null);
	}),

	BIRT,
	BLOB(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media? new FieldRef(parent, "Blob"): null);
	}),
	BURI,

	CALN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof RepositoryRef || parent instanceof Source? new FieldRef(parent, "CallNumber"): null);
	}),
	CAUS(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof EventFact? new FieldRef(parent, "Cause"): null);
	}),
	CENS, CHAN,
	CHAR(node -> {
		final Object parent = node.getParent().getObject();
		CharacterSet obj = null;
		if(parent instanceof Header){
			obj = new CharacterSet();
			((Header)parent).setCharacterSet(obj);
		}
		return obj;
	}),
	CHIL,
	CITY(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "City"): null);
	}),
	CONC(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof FieldRef? parent: new FieldRef(parent, "Value"));
	}),
	CONT(node -> {
		final Object parent = node.getParent().getObject();
		final FieldRef obj = (parent instanceof FieldRef? (FieldRef)parent: new FieldRef(parent, "Value"));
		try{
			obj.appendValue("\n");
		}
		catch(final NoSuchMethodException e){
			throw new RuntimeException("Value not stored for " + node.getTag(), e);
		}
		return obj;
	}),
	COPR(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Header || parent instanceof GeneratorData? new FieldRef(parent, "Copyright"): null);
	}),
	CORP(node -> {
		final Object parent = node.getParent().getObject();
		GeneratorCorporation obj = null;
		if(parent instanceof Generator){
			obj = new GeneratorCorporation();
			((Generator)parent).setGeneratorCorporation(obj);
		}
		return obj;
	}),
	CREM,
	CTRY(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Address)
			obj = new FieldRef(parent, "Country");
		return obj;
	}),
	_CUT, _CUTD,

	DATA(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Generator){
			obj = new GeneratorData();
			((Generator)parent).setGeneratorData((GeneratorData)obj);
		}
		else if(parent instanceof SourceCitation){
			obj = new GeneratorData();
			final SourceCitation.DataTagContents dataTagContents = ((SourceCitation)parent).getDataTagContents();
			if(dataTagContents == SourceCitation.DataTagContents.DATE || dataTagContents == SourceCitation.DataTagContents.TEXT)
				((SourceCitation)parent).setDataTagContents(SourceCitation.DataTagContents.SEPARATE);
		}
		return obj;
	}),
	DATE(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof GeneratorData || parent instanceof Source || parent instanceof EventFact)
			return new FieldRef(parent, "Date");
		else if(parent instanceof SourceCitation){
			//what a hack... - everyone uses the DATA tag differently; some people put only DATE under it, others only TEXT, others both,
			//and still others put DATE and TEXT under two separate DATA tags
			//if the second-from-top-of-stack is also a source citation, then we're skipping a DATA tag and we need to set DataTagContents
			final GedcomNode parentNode = node.getParent();
			final GedcomNode grandparentNode = (parentNode != null? parentNode.getParent(): null);
			if(grandparentNode != null && grandparentNode.getObject() instanceof SourceCitation){
				SourceCitation.DataTagContents dataTagContents = ((SourceCitation)parent).getDataTagContents();
				if(dataTagContents == null)
					dataTagContents = SourceCitation.DataTagContents.DATE;
				else if(dataTagContents == SourceCitation.DataTagContents.TEXT)
					dataTagContents = SourceCitation.DataTagContents.COMBINED;
				((SourceCitation)parent).setDataTagContents(dataTagContents);
			}

			return new FieldRef(parent, "Date");
		}
		else{
			final DateTime dateTime = new DateTime();
			if(parent instanceof Header)
				((Header)parent).setDateTime(dateTime);
			else if(parent instanceof Change)
				((Change)parent).setDateTime(dateTime);
			return dateTime;
		}
	}),
	_DATE, DEAT,
	DESC(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Submission? new FieldRef(parent, "Description"): null);
	}),
	DESI,
	DEST(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Header? new FieldRef(parent, "Destination"): null);
	}),
	DIV, DSCR,

	EDUC,
	EMAIL(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof Submitter)
			((Submitter)parent).setEmailTag(node.getTag());
		else if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setEmailTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setEmailTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setEmailTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setEmailTag(node.getTag());
		return new FieldRef(parent, "Email");
	}),
	_EMAIL(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof Submitter)
			((Submitter)parent).setEmailTag(node.getTag());
		else if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setEmailTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setEmailTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setEmailTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setEmailTag(node.getTag());
		return new FieldRef(parent, "Email");
	}),
	EMIG,
	_EML(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof Submitter)
			((Submitter)parent).setEmailTag(node.getTag());
		else if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setEmailTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setEmailTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setEmailTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setEmailTag(node.getTag());
		return new FieldRef(parent, "Email");
	}),
	ENGA, EVEN,

	FAM(node -> {
		final Object parent = node.getParent().getObject();
		final Family obj = new Family(node.getID());
		((Gedcom)parent).addFamily(obj);
		return obj;
	}),
	FAMC, FAMS,
	FAX(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof GeneratorCorporation || parent instanceof Repository || parent instanceof EventFact
			|| parent instanceof Person || parent instanceof Submitter? new FieldRef(parent, "Fax"): null);
	}),
	_FILE(node -> {
		Object obj = null;
		final Object parent = node.getParent().getObject();
		if(parent instanceof Header)
			obj = new FieldRef(parent, "File");
		else if(parent instanceof Media){
			((Media)parent).setFileTag(node.getTag());
			obj = new FieldRef(parent, "File");
		}
		return obj;
	}),
	FILE(node -> {
		Object obj = null;
		final Object parent = node.getParent().getObject();
		if(parent instanceof Header)
			obj = new FieldRef(parent, "File");
		else if(parent instanceof Media){
			((Media)parent).setFileTag(node.getTag());
			obj = new FieldRef(parent, "File");
		}
		return obj;
	}),
	FONE,
	FORM(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof GedcomVersion)
			obj = new FieldRef(parent, "Form");
		else if(parent instanceof Media)
			obj = new FieldRef(parent, "Format");
		return obj;
	}),
	_FREL,

	GED,
	GEDC(node -> {
		final Object parent = node.getParent().getObject();
		GedcomVersion obj = null;
		if(parent instanceof Header){
			obj = new GedcomVersion();
			((Header)parent).setGedcomVersion(obj);
		}
		return obj;
	}),
	GIVN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Given"): null);
	}),
	GRAD,

	HEAD(node -> {
		final Object parent = node.getParent().getObject();
		final Header obj = new Header();
		((Gedcom)parent).setHeader(obj);
		return obj;
	}),
	HUSB,

	IMMI,
	INDI(node -> {
		final Object parent = node.getParent().getObject();
		final Person obj = new Person(node.getID());
		((Gedcom)parent).addPerson(obj);
		return obj;
	}),
	_ITALIC(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "Italic"): null);
	}),

	LANG(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Submitter || parent instanceof Header? new FieldRef(parent, "Language"): null);
	}),
	LATI, LONG,

	MAP, MARB, MARL, MARR, _MARRNM, _MAR, _MARNM, MEDI, _MREL,

	NAME(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Generator || parent instanceof Repository || parent instanceof Address || parent instanceof Submitter)
			obj = new FieldRef(parent, "Name");
		else if(parent instanceof Person){
			obj = new Name();
			((Person)parent).addName((Name)obj);
		}
		return obj;
	}),
	_NAME(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Generator || parent instanceof Repository || parent instanceof Address || parent instanceof Submitter)
			obj = new FieldRef(parent, "Name");
		else if(parent instanceof Person){
			obj = new Name();
			((Person)parent).addName((Name)obj);
		}
		return obj;
	}),
	NAME_TYPE, NATU, NCHI,
	NICK(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Nickname"): null);
	}),
	NOTE(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof NoteContainer){
			if(node.getXRef() == null){
				obj = new Note();
				((NoteContainer)parent).addNote((Note)obj);
			}
			else{
				obj = new NoteRef(node.getXRef());
				((NoteContainer)parent).addNoteRef(((NoteRef)obj));
			}
		}
		else if(parent instanceof Gedcom){
			obj = new Note(node.getID());
			if(node.getXRef() != null)
				//ref is invalid here, so store it as value - another geni-ism
				((Note)obj).setValue("@" + node.getXRef() + "@");
			((Gedcom)parent).addNote((Note)obj);
		}
		return obj;
	}),
	NPFX(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Prefix"): null);
	}),
	NSFX(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Suffix"): null);
	}),

	OBJE(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof MediaContainer){
			if(node.getXRef() == null){
				obj = new Media();
				((MediaContainer)parent).addMedia((Media)obj);
			}
			else{
				obj = new MediaRef(node.getXRef());
				((MediaContainer)parent).addMediaRef(((MediaRef)obj));
			}
		}
		if(parent instanceof Gedcom){
			obj = new Media(node.getID());
			((Gedcom)parent).addMedia((Media)obj);
		}
		return obj;
	}),
	OCCU,
	ORDI(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Submission? new FieldRef(parent, "OrdinanceFlag"): null);
	}),
	ORDN,

	PAGE(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof SourceCitation? new FieldRef(parent, "Page"): null);
	}),
	_PAREN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "Paren"): null);
	}),
	PEDI(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof ParentFamilyRef? new FieldRef(parent, "RelationshipType"): null);
	}),
	PHON(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof GeneratorCorporation || parent instanceof Repository || parent instanceof EventFact
			|| parent instanceof Person || parent instanceof Submitter? new FieldRef(parent, "Phone"): null);
	}),
	POST(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "PostalCode"): null);
	}),
	PLAC(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof EventFact? new FieldRef(parent, "Place"): null);
	}),
	_PREF(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof SpouseRef? new FieldRef(parent, "Preferred"): null);
	}),
	_PRIM(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media || parent instanceof ParentFamilyRef? new FieldRef(parent, "Primary"): null);
	}),
	_PRIMARY(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media || parent instanceof ParentFamilyRef? new FieldRef(parent, "Primary"): null);
	}),
	PROP,
	PUBL(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "PublicationFacts"): null);
	}),
	_PUBL(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Source? new FieldRef(parent, "PublicationFacts"): null);
	}),

	QUAY(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof SourceCitation? new FieldRef(parent, "Quality"): null);
	}),

	REFN,
	RELA(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Association? new FieldRef(parent, "Relation"): null);
	}),
	RELI,
	REPO(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Source){
			obj = new RepositoryRef(node.getXRef());
			((Source)parent).setRepositoryRef((RepositoryRef)obj);
		}
		else if(parent instanceof Gedcom){
			obj = new Repository(node.getID());
			((Gedcom)parent).addRepository((Repository)obj);
		}
		return obj;
	}),
	RESI, RETI,
	RFN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Person? new FieldRef(parent, "RecordFileNumber"): null);
	}),
	RIN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Submitter || parent instanceof Note || parent instanceof Repository || parent instanceof EventFact
			|| parent instanceof Source || parent instanceof PersonFamilyCommonContainer? new FieldRef(parent, "Rin"): null);
	}),
	ROLE,
	ROMN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Romn"): null);
	}),

	_SCBK(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media? new FieldRef(parent, "Scrapbook"): null);
	}),
	SEX,
	SOUR(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Header){
			obj = new Generator();
			((Header)parent).setGenerator((Generator)obj);
		}
		else if(parent instanceof SourceCitationContainer){
			obj = new SourceCitation(node.getXRef());
			((SourceCitationContainer)parent).addSourceCitation((SourceCitation)obj);
		}
		else if(parent instanceof Note){
			obj = new SourceCitation(node.getXRef());
			((Note)parent).addSourceCitation((SourceCitation)obj);
		}
		else if(parent instanceof NoteRef){
			obj = new SourceCitation(node.getXRef());
			((NoteRef)parent).addSourceCitation((SourceCitation)obj);
		}
		else if(parent instanceof FieldRef && ((FieldRef)parent).getTarget() instanceof Note && "Value".equals(((FieldRef)parent).getFieldName())){
			obj = new SourceCitation(node.getXRef());
			//reunion puts source citations under value: 0 NOTE 1 CONT ... 2 SOUR
			final Note note = (Note)((FieldRef)parent).getTarget();
			note.addSourceCitation((SourceCitation)obj);
			note.setSourceCitationsUnderValue(true);
		}
		else if(parent instanceof Gedcom){
			obj = new Source(node.getID());
			((Gedcom)parent).addSource((Source)obj);
		}
		return obj;
	}),
	SPFX(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "SurnamePrefix"): null);
	}),
	_SSHOW(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media? new FieldRef(parent, "SlideShow"): null);
	}),
	SSN,
	STAE(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "State"): null);
	}),
	STAT(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Address? new FieldRef(parent, "Status"): null);
	}),
	SUBM(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Header && node.getXRef() != null){
			//placeholder
			obj = new Object();
			((Header)parent).setSubmitterRef(node.getXRef());
		}
		if(parent instanceof Gedcom){
			obj = new Submitter(node.getID());
			((Gedcom)parent).addSubmitter((Submitter)obj);
		}
		return obj;
	}),
	SUBN(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Header){
			if(node.getXRef() != null){
				//placeholder
				obj = new Object();
				((Header)parent).setSubmissionRef(node.getXRef());
			}
			else{
				obj = new Submission();
				((Header)parent).setSubmission((Submission)obj);
			}
		}
		else if(parent instanceof Gedcom){
			obj = new Submission(node.getID());
			((Gedcom)parent).setSubmission((Submission)obj);
		}
		return obj;
	}),
	SURN(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Name? new FieldRef(parent, "Surname"): null);
	}),

	TEMP(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof LdsOrdinance? new FieldRef(parent, "Temple"): null);
	}),
	TEXT,
	TIME(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof DateTime? new FieldRef(parent, "Time"): null);
	}),
	TITL(node -> {
		final Object parent = node.getParent().getObject();
		return (parent instanceof Media || parent instanceof Source? new FieldRef(parent, "Title"): null);
	}),
	TRLR(node -> /* do nothing */ null),
	TYPE, _TYPE,

	UID, _UID,
	_URL(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setWwwTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setWwwTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setWwwTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setWwwTag(node.getTag());
		return new FieldRef(parent, "Www");
	}),

	VERS(node -> {
		final Object parent = node.getParent().getObject();
		Object obj = null;
		if(parent instanceof Generator || parent instanceof GedcomVersion || parent instanceof CharacterSet)
			obj = new FieldRef(parent, "Version");
		return obj;
	}),

	WIFE,
	_WEB(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setWwwTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setWwwTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setWwwTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setWwwTag(node.getTag());
		return new FieldRef(parent, "Www");
	}),
	WWW(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setWwwTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setWwwTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setWwwTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setWwwTag(node.getTag());
		return new FieldRef(parent, "Www");
	}),
	_WWW(node -> {
		final Object parent = node.getParent().getObject();
		if(parent instanceof GeneratorCorporation)
			((GeneratorCorporation)parent).setWwwTag(node.getTag());
		else if(parent instanceof Repository)
			((Repository)parent).setWwwTag(node.getTag());
		else if(parent instanceof EventFact)
			((EventFact)parent).setWwwTag(node.getTag());
		else if(parent instanceof Person)
			((Person)parent).setWwwTag(node.getTag());
		return new FieldRef(parent, "Www");
	}),


	//personal LDS ordinances:
	//family LDS ordinances:
	BAPL, CONL, WAC, ENDL, SLGC,

	SLGS;


	private Function<GedcomNode, Object> creator;


	public static GedcomTag from(final String tag){
		for(final GedcomTag t : values())
			if(t.toString().equals(tag))
				return t;
		return null;
	}

//FIXME to remove
GedcomTag(){}

	GedcomTag(final Function<GedcomNode, Object> creator){
		this.creator = creator;
	}

	public void createFrom(final GedcomNode node){
//FIXME to remove
if(creator == null)
	System.out.println(node);
		final Object obj = creator.apply(node);
		node.setObject(obj);
	}

}
