package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


/*
INDIVIDUAL_EVENT_STRUCTURE :=
[
n [ BIRT | CHR ] [Y|<NULL>]    {1:1}
  +1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
  +1 FAMC @<XREF:FAM>@    {0:1}
|
n DEAT [Y|<NULL>]    {1:1}
  +1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
|
n [ BURI | CREM | BAPM | BARM | BASM | BLES | CHRA | CONF | FCOM | ORDN | NATU | EMIG | IMMI | CENS | PROB | WILL | GRAD | RETI | EVEN ]    {1:1}
  +1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
|
n ADOP    {1:1}
  +1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
  +1 FAMC @<XREF:FAM>@    {0:1}
    +2 ADOP <ADOPTED_BY_WHICH_PARENT>    {0:1}
]

*/
public class IndividualEventStructureTransformation implements Transformation{

	private static final Set<String> EVENTS_Y_NULL_FAMILY = new HashSet<>(Arrays.asList("BIRT", "CHR"));
	private static final Set<String> EVENTS_Y_NULL = new HashSet<>(Arrays.asList("DEAT"));
	private static final Set<String> EVENTS = new HashSet<>(Arrays.asList("BURI", "CREM", "BAPM", "BARM", "BASM", "BLES", "CHRA", "CONF",
		"FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI", "EVEN"));
	private static final Set<String> EVENTS_FAMILY = new HashSet<>(Arrays.asList("ADOP"));

	private static final Transformation INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION = new IndividualEventDetailTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			if(EVENTS_Y_NULL_FAMILY.contains(tag)){
				final String value = child.getValue();
				child.withValue("BIRT".equals(value)? "BIRTH": "_CHR");
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
				moveTag("FAMILY_CHILD", child, "FAMC");
			}
			else if(EVENTS_Y_NULL.contains(tag)){
				child.withValue("DEATH");
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(EVENTS.contains(tag)){
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
				//TODO
			}
			else if(EVENTS_FAMILY.contains(tag)){
				child.withValue("ADOPTION");
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
				moveTag("ADOPTED_BY", child, "FAMC", "ADOP");
				moveTag("FAMILY_CHILD", child, "FAMC");
			}
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		//TODO
	}

}
