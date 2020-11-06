package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class FamilyEventStructureTransformation implements Transformation{

	private enum EventTag{
		ANUL("ANNULMENT"), CENS("CENSUS"), DIV("DIVORCE"), DIVF("DIVORCE_FILED"), ENGA("ENGAGEMENT"),
		MARB("MARRIAGE_BANN"), MARC("MARRIAGE_CONTRACT"), MARL("MARRIAGE_LICENCE"), MARS("MARRIAGE_SETTLEMENT"),
		RESI("RESIDENCE");

		private final String code;

		static EventTag fromTag(final String tag){
			for(final EventTag et : values())
				if(et.toString().equals(tag))
					return et;
			return null;
		}

		static EventTag fromCode(final String code){
			for(final EventTag et : values())
				if(et.code.equals(code))
					return et;
			return null;
		}

		EventTag(final String code){
			this.code = code;
		}
	}

	private enum EventTagYNull{
		MARR("MARRIAGE");

		private final String code;

		static EventTagYNull fromTag(final String tag){
			for(final EventTagYNull etynf : values())
				if(etynf.toString().equals(tag))
					return etynf;
			return null;
		}

		static EventTagYNull fromCode(final String code){
			for(final EventTagYNull etyn : values())
				if(etyn.code.equals(code))
					return etyn;
			return null;
		}

		EventTagYNull(final String code){
			this.code = code;
		}
	}

	private static final Transformation FAMILY_EVENT_DETAIL_TRANSFORMATION = new FamilyEventDetailTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			final EventTag et = EventTag.fromTag(tag);
			final EventTagYNull etynf = EventTagYNull.fromTag(tag);
			if(et != null){
				child.withTag("EVENT");
				child.withValue(et.code);
				FAMILY_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(etynf != null){
				child.withTag("EVENT");
				child.withValue(etynf.code);
				FAMILY_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(child.getTag().equals("EVEN")){
				child.withTag("EVENT");
				FAMILY_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren())
			if("EVENT".equals(child.getTag())){
				final String code = child.getValue();
				final EventTag et = EventTag.fromCode(code);
				final EventTagYNull etynf = EventTagYNull.fromCode(code);
				if(et != null){
					child.withTag(et.toString());
					child.removeValue();
					FAMILY_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				}
				else if(etynf != null){
					child.withTag(etynf.toString());
					child.removeValue();
					FAMILY_EVENT_DETAIL_TRANSFORMATION.from(child, root);
					moveTag("FAMILY_CHILD", child, "FAMC");
				}
				else{
					child.withTag("CHILDREN_COUNT".equals(child.getValue())? "_EVEN": "EVEN");
					FAMILY_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				}
			}
	}

}
