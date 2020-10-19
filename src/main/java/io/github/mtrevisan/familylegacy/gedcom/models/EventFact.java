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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class EventFact extends SourceCitationContainer{

	public static final Set<String> PERSONAL_EVENT_FACT_TAGS = new HashSet<>(Arrays.asList("ADOP", "ADOPTION", "ADULT_CHRISTNG", "AFN", "ARRI", "ARVL", "ARRIVAL", "_ATTR", "BAP", "BAPM", "BAPT", "BAPTISM", "BARM", "BAR_MITZVAH", "BASM", "BAS_MITZVAH", "BATM", "BAT_MITZVAH", "BIRT", "BIRTH", "BLES", "BLESS", "BLESSING", "BLSL", "BURI", "BURIAL", "CAST", "CASTE", "CAUS", "CENS", "CENSUS", "CHILDREN_COUNT", "CHR", "CHRA", "CHRISTENING", "CIRC", "CITN", "_COLOR", "CONF", "CONFIRMATION", "CREM", "CREMATION", "_DCAUSE", "DEAT", "DEATH", "_DEATH_OF_SPOUSE", "DEED", "_DEG", "_DEGREE", "DEPA", "DPRT", "DSCR", "DWEL", "EDUC", "EDUCATION", "_ELEC", "EMAIL", "EMIG", "EMIGRATION", "EMPL", "_EMPLOY", "ENGA", "ENLIST", "EVEN", "EVENT", "_EXCM", "EXCO", "EYES", "FACT", "FCOM", "FIRST_COMMUNION", "_FNRL", "_FUN", "_FA1", "_FA2", "_FA3", "_FA4", "_FA5", "_FA6", "_FA7", "_FA8", "_FA9", "_FA10", "_FA11", "_FA12", "_FA13", "GRAD", "GRADUATION", "HAIR", "HEIG", "_HEIG", "_HEIGHT", "IDNO", "IDENT_NUMBER", "_INTE", "ILL", "ILLN", "IMMI", "IMMIGRATION", "LVG", "LVNG", "MARR", "MARRIAGE_COUNT", "_MDCL", "_MEDICAL", "MIL", "_MIL", "MILA", "MILD", "MILI", "_MILI", "MILT", "_MILT", "_MILTID", "MISE", "_MISE", "_MILITARY_SERVICE", "MISN ", "_MISN", "MOVE", "_NAMS", "NATI", "NATIONALITY", "NATU", "NATURALIZATION", "NCHI", "NMR", "OCCU", "OCCUPATION", "ORDI", "ORDL", "ORDN", "ORDINATION", "PHON", "PHY_DESCRIPTION", "PROB", "PROBATE", "PROP", "PROPERTY", "RACE", "RELI", "RELIGION", "RESI", "RESIR", "RESIDENCE", "RETI", "RETIREMENT", "SEX", "SOC_SEC_NUMBER", "SSN", "STIL", "STLB", "TITL", "TITLE", "WEIG", "_WEIG", "_WEIGHT", "WILL"));
	public static final Set<String> FAMILY_EVENT_FACT_TAGS = new HashSet<>(Arrays.asList("ANUL", "CENS", "CLAW", "_DEATH_OF_SPOUSE", "DIV", "DIVF", "DIVORCE", "_DIV", "EMIG", "ENGA", "EVEN", "EVENT", "IMMI", "MARB", "MARC", "MARL", "MARR", "MARRIAGE", "MARS", "_MBON", "NCHI", "RESI", "SEPA", "_SEPR", "_SEPARATED"));

	public static final Map<String, String> DISPLAY_TYPE;
	static{
		//note: some of these tags aren't in the personal/family_event_fact_tags sets because they appear only in the type field
		//others need to be added to the appropriate tag sets
		final Map<String, String> m = new HashMap<>();
		m.put("ADOP", "Adoption");
		m.put("ADOPTION", "Adoption");
		m.put("AFN", "Ancestral file number");
		m.put("ANUL", "Annulment");
		m.put("ANNULMENT", "Annulment");
		m.put("ARRIVAL", "Arrival");
		m.put("ARRI", "Arrival");
		m.put("ARVL", "Arrival");
		m.put("_ATTR", "Attribute");
		m.put("BAP", "Baptism");
		m.put("BAPM", "Baptism");
		m.put("BAPT", "Baptism");
		m.put("BAPTISM", "Baptism");
		m.put("BARM", "Bar mitzvah");
		m.put("BATM", "Bat mitzvah");
		m.put("BAR_MITZVAH", "Bar mitzvah");
		m.put("BIRT", "Birth");
		m.put("BIRTH", "Birth");
		m.put("BLES", "Blessing");
		m.put("BURI", "Burial");
		m.put("BURIAL", "Burial");
		m.put("CAST", "Caste");
		m.put("CAUS", "Cause of death");
		m.put("CAUSE", "Cause of death");
		m.put("CENS", "Census");
		m.put("CHR", "Christening");
		m.put("CHRISTENING", "Christening");
		m.put("CLAW", "Common law marriage");
		m.put("_COLOR", "Color");
		m.put("CONF", "Confirmation");
		m.put("CREM", "Cremation");
		m.put("_DCAUSE", "Cause of death");
		m.put("DEAT", "Death");
		m.put("DEATH", "Death");
		m.put("_DEATH_OF_SPOUSE", "Death of spouse");
		m.put("DEED", "Deed");
		m.put("_DEG", "Degree");
		m.put("_DEGREE", "Degree");
		m.put("DEPA", "Departure");
		m.put("DPRT", "Departure");
		m.put("DIV", "Divorce");
		m.put("DIVF", "Divorce filing");
		m.put("DIVORCE", "Divorce");
		m.put("_DIV", "Divorce");
		m.put("DSCR", "Physical description");
		m.put("EDUC", "Education");
		m.put("EDUCATION", "Education");
		m.put("_ELEC", "Elected");
		m.put("EMAIL", "Email");
		m.put("EMIG", "Emigration");
		m.put("EMIGRATION", "Emigration");
		m.put("EMPL", "Employment");
		m.put("_EMPLOY", "Employment");
		m.put("ENGA", "Engagement");
		m.put("ENLIST", "Military");
		m.put("EVEN", "Event");
		m.put("EVENT", "Event");
		m.put("EYES", "Eyes");
		m.put("_EXCM", "Excommunication");
		m.put("FCOM", "First communion");
		m.put("_FNRL", "Funeral");
		m.put("_FUN", "Funeral");
		m.put("GRAD", "Graduation");
		m.put("GRADUATION", "Graduation");
		m.put("HAIR", "Hair");
		m.put("HEIG", "Height");
		m.put("_HEIG", "Height");
		m.put("_HEIGHT", "Height");
		m.put("ILL", "Illness");
		m.put("IMMI", "Immigration");
		m.put("IMMIGRATION", "Immigration");
		m.put("MARB", "Marriage banns");
		m.put("MARC", "Marriage contract");
		m.put("MARL", "Marriage license");
		m.put("MARR", "Marriage");
		m.put("MARRIAGE", "Marriage");
		m.put("MARS", "Marriage settlement");
		m.put("_MBON", "Marriage banns");
		m.put("_MDCL", "Medical");
		m.put("_MEDICAL", "Medical");
		m.put("MIL", "Military");
		m.put("_MIL", "Military");
		m.put("MILI", "Military");
		m.put("_MILI", "Military");
		m.put("_MILT", "Military");
		m.put("_MILTID", "Military");
		m.put("_MILITARY_SERVICE", "Military");
		m.put("MISE", "Military");
		m.put("_MISN", "Mission");
		m.put("_NAMS", "Namesake");
		m.put("NATI", "Nationality");
		m.put("NATU", "Naturalization");
		m.put("NATURALIZATION", "Naturalization");
		m.put("NCHI", "Number of children");
		m.put("OCCU", "Occupation");
		m.put("OCCUPATION", "Occupation");
		m.put("ORDI", "Ordination");
		m.put("ORDN", "Ordination");
		m.put("PHON", "Phone");
		m.put("PROB", "Probate");
		m.put("PROP", "Property");
		m.put("RELI", "Religion");
		m.put("RELIGION", "Religion");
		m.put("RESI", "Residence");
		m.put("RESIDENCE", "Residence");
		m.put("RETI", "Retirement");
		m.put("SEPA", "Separated");
		m.put("_SEPARATED", "Separated");
		m.put("_SEPR", "Separated");
		m.put("SEX", "Sex");
		m.put("SSN", "Social security number");
		m.put("SOC_", "Social security number");
		m.put("SOC_SEC_NUMBER", "Social security number");
		m.put("TITL", "Title");
		m.put("TITLE", "Title");
		m.put("_WEIG", "Weight");
		m.put("_WEIGHT", "Weight");
		m.put("WILL", "Will");
		DISPLAY_TYPE = Collections.unmodifiableMap(m);
	}

	public static final String OTHER_TYPE = "Other";

	private String value;
	private String tag;
	private String type;
	private String date;
	private String place;
	private Address addr;
	private String phon;
	private String fax;
	private String rin;
	private String caus;
	private String _uid;
	private String uidTag;
	private String _email;
	private String emailTag;
	private String _www;
	private String wwwTag;


	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	/**
	 * Return human-friendly event type.
	 *
	 * @return	Human-friendly event type
	 */
	public String getDisplayType(){
		if(tag != null){
			final String displayType = DISPLAY_TYPE.get(tag.toUpperCase());
			if(displayType != null)
				return displayType;
		}
		return OTHER_TYPE;
	}

	public String getTag(){
		return tag;
	}

	public void setTag(final String tag){
		this.tag = tag;
	}

	public String getType(){
		return type;
	}

	public void setType(final String type){
		this.type = type;
	}

	public String getDate(){
		return date;
	}

	public void setDate(final String date){
		this.date = date;
	}

	public String getPlace(){
		return place;
	}

	public void setPlace(final String place){
		this.place = place;
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

	public String getCause(){
		return caus;
	}

	public void setCause(final String caus){
		this.caus = caus;
	}

	public String getRin(){
		return rin;
	}

	public void setRin(final String rin){
		this.rin = rin;
	}

	public String getUid(){
		return _uid;
	}

	public void setUid(final String uid){
		_uid = uid;
	}

	public String getUidTag(){
		return uidTag;
	}

	public void setUidTag(final String uidTag){
		this.uidTag = uidTag;
	}

	public String getEmail(){
		return _email;
	}

	public void setEmail(final String email){
		_email = email;
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
//	@Override
//	public void visitContainedObjects(final Visitor visitor){
//		if(addr != null)
//			addr.accept(visitor);
//
//		super.visitContainedObjects(visitor);
//	}

	//FIXME
//	@Override
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			this.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
