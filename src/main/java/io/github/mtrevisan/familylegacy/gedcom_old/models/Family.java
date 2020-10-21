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
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Family extends PersonFamilyCommonContainer{

	private final String id;
	private List<SpouseRef> husbandRefs;
	private List<SpouseRef> wifeRefs;
	private List<ChildRef> childRefs;


	public Family(final String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	private List<Person> getFamilyMembers(final Gedcom gedcom, final Collection<? extends SpouseRef> memberRefs,
			final boolean preferredFirst){
		final List<Person> members = new ArrayList<>(memberRefs.size());
		for(final SpouseRef memberRef : memberRefs){
			final Person member = memberRef.getPerson(gedcom);
			if(member != null){
				if(preferredFirst && "Y".equals(memberRef.getPreferred()))
					members.add(0, member);
				else
					members.add(member);
			}
		}
		return members;
	}

	/**
	 * Convenience function to dereference husband refs.
	 *
	 * @param gedcom	Gedcom
	 * @return	List of husbands (preferred in first position)
	 */
	public List<Person> getHusbands(final Gedcom gedcom){
		return getFamilyMembers(gedcom, getHusbandRefs(), true);
	}

	public List<SpouseRef> getHusbandRefs(){
		return (husbandRefs != null? husbandRefs: Collections.emptyList());
	}

	public void setHusbandRefs(final List<SpouseRef> husbandRefs){
		this.husbandRefs = husbandRefs;
	}

	public void addHusband(final SpouseRef husband){
		if(husbandRefs == null)
			husbandRefs = new ArrayList<>(1);

		husbandRefs.add(husband);
	}

	/**
	 * Convenience function to dereference wife refs.
	 *
	 * @param gedcom	Gedcom
	 * @return	List of wives (preferred in first position)
	 */
	public List<Person> getWives(final Gedcom gedcom){
		return getFamilyMembers(gedcom, getWifeRefs(), true);
	}

	public List<SpouseRef> getWifeRefs(){
		return (wifeRefs != null? wifeRefs: Collections.emptyList());
	}

	public void setWifeRefs(final List<SpouseRef> wifeRefs){
		this.wifeRefs = wifeRefs;
	}

	public void addWife(final SpouseRef wife){
		if(wifeRefs == null)
			wifeRefs = new ArrayList<>(1);

		wifeRefs.add(wife);
	}

	/**
	 * Convenience function to dereference child refs.
	 *
	 * @param gedcom	Gedcom
	 * @return	List of children
	 */
	public List<Person> getChildren(final Gedcom gedcom){
		return getFamilyMembers(gedcom, getChildRefs(), false);
	}

	public List<ChildRef> getChildRefs(){
		return (childRefs != null? childRefs: Collections.emptyList());
	}

	public void setChildRefs(final List<ChildRef> childRefs){
		this.childRefs = childRefs;
	}

	public void addChild(final ChildRef childRef){
		if(childRefs == null)
			childRefs = new ArrayList<>(1);

		childRefs.add(childRef);
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			for(final SpouseRef husband : getHusbandRefs())
//				husband.accept(visitor, true);
//			for(final SpouseRef wife : getWifeRefs())
//				wife.accept(visitor, false);
//			for(final ChildRef childRef : getChildRefs())
//				childRef.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
