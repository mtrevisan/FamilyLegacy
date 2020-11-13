package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class FamilyTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordTo(family, destination);
	}

	private void familyRecordTo(final GedcomNode family, final Flef destination){
//	n @<XREF:FAM>@ FAM    {1:1}	/* An xref ID of a family record. */
//		+1 RESN <RESTRICTION_NOTICE>    {0:1}	/* The restriction notice is defined for Ancestral File usage. Ancestral File download GEDCOM files may contain this data. */
//		+1 <<FAMILY_EVENT_STRUCTURE>>    {0:M}	/* A list of FAMILY_EVENT_STRUCTURE() objects giving events associated with this family. */
//		+1 HUSB @<XREF:INDI>@    {0:1}	/* An xref ID of the husband. */
//		+1 WIFE @<XREF:INDI>@    {0:1}	/* An xref ID of the wife. */
//		+1 CHIL @<XREF:INDI>@    {0:M}	/* A vector of xref IDs of children in this family. */
//		+1 NCHI <COUNT_OF_CHILDREN>    {0:1}	/* The known number of children of this individual from all marriages or, if subordinate to a family record, the reported number of children known to belong to this family, regardless of whether the associated children are represented in the corresponding structure. This is not necessarily the count of children listed in a family structure. */
//		+1 SUBM @<XREF:SUBM>@    {0:M}	/* A vector of xref IDs of submitters of this record. */
//		+1 <<LDS_SPOUSE_SEALING>>    {0:M}
//		+1 REFN <USER_REFERENCE_NUMBER>    {0:M}	/* A user-defined number or text that the submitter uses to identify this record. */
//			+2 TYPE <USER_REFERENCE_TYPE>    {0:1}	/* A user-defined definition of the user_reference_number. */
//		+1 RIN <AUTOMATED_RECORD_ID>    {0:1}	/* A unique record identification number assigned to the record by the source system. This number is intended to serve as a more sure means of identification of a record for reconciling differences in data between two interfacing systems. */
//		+1 <<CHANGE_DATE>>    {0:1}	/* A CHANGE_DATE() object giving the time this record was last modified. If not provided, the current date is used. */
//		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects */

//		 @<XREF:FAMILY>@ FAMILY    {1:1}	/* An xref ID of a family record. */
//		+1 SPOUSE1 @<XREF:INDIVIDUAL>@    {0:1}	/* An xref ID of the first spouse. In a heterosexual pair union, this is traditionally the husband or father. */
//		+1 SPOUSE2 @<XREF:INDIVIDUAL>@    {0:1}	/* An xref ID of the second spouse. In a heterosexual pair union, this is traditionally the wife or mother. */
//		+1 CHILD @<XREF:INDIVIDUAL>@    {0:M}	/* A vector of xref IDs of children in this family. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 <<FAMILY_EVENT_STRUCTURE>>    {0:M}	/* A list of FAMILY_EVENT_STRUCTURE() objects giving events associated with this family. */
//		+1 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */

		final GedcomNode destinationFamily = GedcomNode.create("FAMILY")
			.withID(family.getID());
//		personalNameTo(family, destinationFamily, destination);
//		destinationFamily.addChildValue("SEX", extractSubStructure(family, "SEX")
//				.getValue());
//		childToFamilyLinkTo(family, destinationFamily, destination);
//		spouseToFamilyLinkTo(family, destinationFamily, destination);
//		associationTo(family, destinationFamily, destination);
//		aliasTo(family, destinationFamily, destination);
//		eventTo(family, destinationFamily, destination, "BIRT", "BIRTH");
//		eventTo(family, destinationFamily, destination, "ADOP", "ADOPTION");
//		eventTo(family, destinationFamily, destination, "DEAT", "DEATH");
//		eventTo(family, destinationFamily, destination, "BURI", "BURIAL");
//		eventTo(family, destinationFamily, destination, "CREM", "CREMATION");
//		eventTo(family, destinationFamily, destination, "NATU", "NATURALIZATION");
//		eventTo(family, destinationFamily, destination, "EMIG", "EMIGRATION");
//		eventTo(family, destinationFamily, destination, "IMMI", "IMMIGRATION");
//		eventTo(family, destinationFamily, destination, "CENS", "CENSUS");
//		eventTo(family, destinationFamily, destination, "PROB", "PROBATE");
//		eventTo(family, destinationFamily, destination, "WILL", "WILL");
//		eventTo(family, destinationFamily, destination, "GRAD", "GRADUATION");
//		eventTo(family, destinationFamily, destination, "RETI", "RETIREMENT");
//		eventTo(family, destinationFamily, destination, "EVEN", "EVENT");
//		attributeTo(family, destinationFamily, destination, "CAST", "CASTE");
//		attributeTo(family, destinationFamily, destination, "DSCR", "CHARACTERISTIC");
//		attributeTo(family, destinationFamily, destination, "EDUC", "EDUCATION");
//		attributeTo(family, destinationFamily, destination, "NATI", "ORIGIN");
//		attributeTo(family, destinationFamily, destination, "NCHI", "CHILDREN_COUNT");
//		attributeTo(family, destinationFamily, destination, "NMR", "MARRIAGES_COUNT");
//		attributeTo(family, destinationFamily, destination, "OCCU", "OCCUPATION");
//		attributeTo(family, destinationFamily, destination, "PROP", "POSSESSION");
//		attributeTo(family, destinationFamily, destination, "RELI", "RELIGION");
//		attributeTo(family, destinationFamily, destination, "RESI", "RESIDENCE");
//		attributeTo(family, destinationFamily, destination, "SSN", "SSN");
//		attributeTo(family, destinationFamily, destination, "TITL", "TITLE");
//		attributeTo(family, destinationFamily, destination, "FACT", null);
//		notesTo(family, destinationFamily, destination);
//		sourceCitationTo(family, destinationFamily, destination);
//		documentsTo(family, destinationFamily, destination);
//		destinationFamily.addChildValue("RESTRICTION", extractSubStructure(family, "RESN")
//			.getValue());
		destination.addFamily(destinationFamily);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordFrom(family, origin, destination);
	}

	private void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination){
		final GedcomNode destinationFamily = GedcomNode.create("FAM")
			.withID(family.getID());
//		destinationFamily.addChildValue("RESN", extractSubStructure(family, "RESTRICTION")
//			.getValue());
//		personalNameFrom(family, destinationFamily);
//		destinationFamily.addChildValue("SEX", extractSubStructure(family, "SEX")
//			.getValue());
//		childToFamilyLinkFrom(family, destinationFamily);
//		spouseToFamilyLinkFrom(family, destinationFamily);
//		associationFrom(family, destinationFamily);
//		aliasFrom(family, destinationFamily);
//		final List<GedcomNode> events = family.getChildrenWithTag("EVENT");
//		eventFrom(events, destinationFamily, origin, "BIRTH", "BIRT");
//		eventFrom(events, destinationFamily, origin, "ADOPTION", "ADOP");
//		eventFrom(events, destinationFamily, origin, "DEATH", "DEAT");
//		eventFrom(events, destinationFamily, origin, "BURIAL", "BURI");
//		eventFrom(events, destinationFamily, origin, "CREMATION", "CREM");
//		eventFrom(events, destinationFamily, origin, "NATURALIZATION", "NATU");
//		eventFrom(events, destinationFamily, origin, "EMIGRATION", "EMIG");
//		eventFrom(events, destinationFamily, origin, "IMMIGRATION", "IMMI");
//		eventFrom(events, destinationFamily, origin, "CENSUS", "CENS");
//		eventFrom(events, destinationFamily, origin, "PROBATE", "PROB");
//		eventFrom(events, destinationFamily, origin, "WILL", "WILL");
//		eventFrom(events, destinationFamily, origin, "GRADUATION", "GRAD");
//		eventFrom(events, destinationFamily, origin, "RETIREMENT", "RETI");
//		eventFrom(events, destinationFamily, origin, "EVENT", "EVEN");
//		final List<GedcomNode> attributes = family.getChildrenWithTag("ATTRIBUTE");
//		attributeFrom(attributes, destinationFamily, origin, "CASTE", "CAST");
//		attributeFrom(attributes, destinationFamily, origin, "CHARACTERISTIC", "DSCR");
//		attributeFrom(attributes, destinationFamily, origin, "EDUCATION", "EDUC");
//		attributeFrom(attributes, destinationFamily, origin, "ORIGIN", "NATI");
//		attributeFrom(attributes, destinationFamily, origin, "CHILDREN_COUNT", "NCHI");
//		attributeFrom(attributes, destinationFamily, origin, "MARRIAGES_COUNT", "NMR");
//		attributeFrom(attributes, destinationFamily, origin, "OCCUPATION", "OCCU");
//		attributeFrom(attributes, destinationFamily, origin, "POSSESSION", "PROP");
//		attributeFrom(attributes, destinationFamily, origin, "RELIGION", "RELI");
//		attributeFrom(attributes, destinationFamily, origin, "RESIDENCE", "RESI");
//		attributeFrom(attributes, destinationFamily, origin, "SSN", "SSN");
//		attributeFrom(attributes, destinationFamily, origin, "TITLE", "TITL");
//		attributeFrom(attributes, destinationFamily, origin, "FACT", "FACT");
//		notesFrom(family, destinationFamily);
//		sourceCitationFrom(family, destinationFamily);
//		documentsFrom(family, destinationFamily);
		destination.addFamily(destinationFamily);
	}

}
