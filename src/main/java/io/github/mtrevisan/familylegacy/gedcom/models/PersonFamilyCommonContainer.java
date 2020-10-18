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
import java.util.Collections;
import java.util.List;


public abstract class PersonFamilyCommonContainer extends SourceCitationContainer{

	private List<EventFact> eventsFacts;
	private List<LdsOrdinance> ldsOrdinances;
	private List<String> refns;
	private String rin;
	private Change chan;
	private String _uid;
	private String uidTag;


	public List<EventFact> getEventsFacts(){
		return (eventsFacts != null? eventsFacts: Collections.emptyList());
	}

	public void setEventsFacts(final List<EventFact> eventsFacts){
		this.eventsFacts = eventsFacts;
	}

	public void addEventFact(final EventFact eventFact){
		if(eventsFacts == null)
			eventsFacts = new ArrayList<>(1);

		eventsFacts.add(eventFact);
	}

	public List<LdsOrdinance> getLdsOrdinances(){
		return (ldsOrdinances != null? ldsOrdinances: Collections.emptyList());
	}

	public void setLdsOrdinances(final List<LdsOrdinance> ldsOrdinances){
		this.ldsOrdinances = ldsOrdinances;
	}

	public void addLdsOrdinance(final LdsOrdinance ldsOrdinance){
		if(ldsOrdinances == null)
			ldsOrdinances = new ArrayList<>(1);

		ldsOrdinances.add(ldsOrdinance);
	}

	public List<String> getReferenceNumbers(){
		return (refns != null? refns: Collections.emptyList());
	}

	public void setReferenceNumbers(final List<String> refns){
		this.refns = refns;
	}

	public void addReferenceNumber(final String refn){
		if(refns == null)
			refns = new ArrayList<>(1);

		refns.add(refn);
	}

	public String getRin(){
		return rin;
	}

	public void setRin(final String rin){
		this.rin = rin;
	}

	public Change getChange(){
		return chan;
	}

	public void setChange(final Change chan){
		this.chan = chan;
	}

	public String getUid(){
		return _uid;
	}

	public void setUid(final String uid){
		this._uid = uid;
	}

	public String getUidTag(){
		return uidTag;
	}

	public void setUidTag(final String uidTag){
		this.uidTag = uidTag;
	}

	//FIXME
//	public void visitContainedObjects(final Visitor visitor){
//		for(final EventFact eventFact : getEventsFacts())
//			eventFact.accept(visitor);
//		for(final LdsOrdinance ldsOrdinance : getLdsOrdinances())
//			ldsOrdinance.accept(visitor);
//		if(chan != null)
//			chan.accept(visitor);
//
//		super.visitContainedObjects(visitor);
//	}

}
