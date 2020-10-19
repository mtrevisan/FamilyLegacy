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
package io.github.mtrevisan.familylegacy.gedcom.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Person extends PersonFamilyCommonContainer{

	private final String id;
	private List<Name> names;
	private List<ParentFamilyRef> famc;
	private List<SpouseFamilyRef> fams;
	private List<Association> associations;
	private String anci;
	private String desi;
	private String rfn;
	private Address addr;
	private String phon;
	private String fax;
	private String email;
	private String emailTag;
	private String _www;
	private String wwwTag;


	public Person(final String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	public List<Name> getNames(){
		return (names != null? names: Collections.emptyList());
	}

	public void setNames(final List<Name> names){
		this.names = names;
	}

	public void addName(final Name name){
		if(names == null)
			names = new ArrayList<>(1);

		names.add(name);
	}

	private List<Family> getFamilies(final Gedcom gedcom, final Collection<? extends SpouseFamilyRef> familyRefs){
		final List<Family> families = new ArrayList<>(familyRefs.size());
		for(final SpouseFamilyRef familyRef : familyRefs){
			final Family family = familyRef.getFamily(gedcom);
			if(family != null)
				families.add(family);
		}
		return families;
	}

	/**
	 * Convenience function to dereference parent family refs.
	 *
	 * @param gedcom	Gedcom
	 * @return	List of parent families
	 */
	public List<Family> getParentFamilies(final Gedcom gedcom){
		return getFamilies(gedcom, getParentFamilyRefs());
	}

	public List<ParentFamilyRef> getParentFamilyRefs(){
		return (famc != null? famc: Collections.emptyList());
	}

	public void setParentFamilyRefs(final List<ParentFamilyRef> famc){
		this.famc = famc;
	}

	public void addParentFamilyRef(final ParentFamilyRef parentFamilyRef){
		if(famc == null)
			famc = new ArrayList<>(1);

		famc.add(parentFamilyRef);
	}

	/**
	 * Convenience function to dereference spouse family refs.
	 *
	 * @param gedcom	Gedcom
	 * @return	List of spouse families
	 */
	public List<Family> getSpouseFamilies(final Gedcom gedcom){
		return getFamilies(gedcom, getSpouseFamilyRefs());
	}

	public List<SpouseFamilyRef> getSpouseFamilyRefs(){
		return (fams != null? fams: Collections.emptyList());
	}

	public void setSpouseFamilyRefs(final List<SpouseFamilyRef> fams){
		this.fams = fams;
	}

	public void addSpouseFamilyRef(final SpouseFamilyRef spouseFamilyRef){
		if(fams == null)
			fams = new ArrayList<>(1);

		fams.add(spouseFamilyRef);
	}

	public List<Association> getAssociations(){
		return (associations != null? associations: Collections.emptyList());
	}

	public void setAssociations(final List<Association> associations){
		this.associations = associations;
	}

	public void addAssociation(final Association association){
		if(associations == null)
			associations = new ArrayList<>(1);

		associations.add(association);
	}

	public String getAncestorInterestSubmitterRef(){
		return anci;
	}

	public void setAncestorInterestSubmitterRef(final String anci){
		this.anci = anci;
	}

	public String getDescendantInterestSubmitterRef(){
		return desi;
	}

	public void setDescendantInterestSubmitterRef(final String desi){
		this.desi = desi;
	}

	public String getRecordFileNumber(){
		return rfn;
	}

	public void setRecordFileNumber(final String rfn){
		this.rfn = rfn;
	}

	public Address getAddress(){
		return addr;
	}

	public void setAddress(final Address addr){
		this.addr = addr;
	}

	public String getPhone(){
		return phon;
	}

	public void setPhone(final String phon){
		this.phon = phon;
	}

	public String getFax(){
		return fax;
	}

	public void setFax(final String fax){
		this.fax = fax;
	}

	public String getEmail(){
		return email;
	}

	public void setEmail(final String email){
		this.email = email;
	}

	public String getEmailTag(){
		return emailTag;
	}

	public void setEmailTag(final String emailTag){
		this.emailTag = emailTag;
	}

	public String getWww(){
		return _www;
	}

	public void setWww(final String www){
		_www = www;
	}

	public String getWwwTag(){
		return wwwTag;
	}

	public void setWwwTag(final String wwwTag){
		this.wwwTag = wwwTag;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			for(final Name name : getNames())
//				name.accept(visitor);
//			for(final ParentFamilyRef parentFamilyRef : getParentFamilyRefs())
//				parentFamilyRef.accept(visitor);
//			for(final SpouseFamilyRef spouseFamilyRef : getSpouseFamilyRefs())
//				spouseFamilyRef.accept(visitor);
//			for(final Association association : getAssociations())
//				association.accept(visitor);
//			if(addr != null)
//				addr.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
