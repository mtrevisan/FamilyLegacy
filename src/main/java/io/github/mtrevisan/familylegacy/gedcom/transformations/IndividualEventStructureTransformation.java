package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class IndividualEventStructureTransformation implements Transformation{

	private enum EventTag{
		DEAT("DEATH"),
		BURI("BURIAL"), CREM("CREMATION"), BAPM("_BAPM"), BARM("_BARM"), BASM("_BASM"), BLES("_BLES"),
		CHRA("_CHRA"), CONF("_CONF"), FCOM("_FCOM"), ORDN("_ORDNM"), NATU("NATURALIZATION"),
		EMIG("EMIGRATION"), IMMI("IMMIGRATION"), CENS("CENSUS"), PROB("PROBATE"), WILL("WILL"),
		GRAD("GRADUATION"), RETI("RETIREMENT");

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

	private enum EventTagYNullFamily{
		BIRT("BIRTH"), CHR("_CHR");

		private final String code;

		static EventTagYNullFamily fromTag(final String tag){
			for(final EventTagYNullFamily etynf : values())
				if(etynf.toString().equals(tag))
					return etynf;
			return null;
		}

		static EventTagYNullFamily fromCode(final String code){
			for(final EventTagYNullFamily etynf : values())
				if(etynf.code.equals(code))
					return etynf;
			return null;
		}

		EventTagYNullFamily(final String code){
			this.code = code;
		}
	}

	private enum EventTagFamily{
		ADOP("ADOPTION");

		private final String code;

		static EventTagFamily fromTag(final String tag){
			for(final EventTagFamily etf : values())
				if(etf.toString().equals(tag))
					return etf;
			return null;
		}

		static EventTagFamily fromCode(final String code){
			for(final EventTagFamily etf : values())
				if(etf.code.equals(code))
					return etf;
			return null;
		}

		EventTagFamily(final String code){
			this.code = code;
		}
	}

	private static final Transformation INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION = new IndividualEventDetailTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			final EventTag et = EventTag.fromTag(tag);
			final EventTagYNullFamily etynf = EventTagYNullFamily.fromTag(tag);
			final EventTagFamily eft = EventTagFamily.fromTag(tag);
			if(et != null){
				child.withTag("EVENT");
				child.withValue(et.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(etynf != null){
				child.withTag("EVENT");
				child.withValue(etynf.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
				moveTag("FAMILY_CHILD", child, "FAMC");
			}
			else if(eft != null){
				child.withTag("EVENT");
				child.withValue(eft.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
				moveTag("ADOPTED_BY", child, "FAMC", "ADOP");
				moveTag("FAMILY_CHILD", child, "FAMC");
			}
			else if("EVEN".equals(child.getTag())){
				child.withTag("EVENT");
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildrenWithTag("EVENT")){
			final String code = child.getValue();
			final EventTag et = EventTag.fromCode(code);
			final EventTagYNullFamily etynf = EventTagYNullFamily.fromCode(code);
			final EventTagFamily eft = EventTagFamily.fromCode(code);
			if(et != null){
				child.withTag(et.toString());
				child.removeValue();
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
			}
			else if(etynf != null){
				child.withTag(etynf.toString());
				child.removeValue();
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				moveTag("FAMILY_CHILD", child, "FAMC");
			}
			else if(eft != null){
				child.withTag(eft.toString());
				child.removeValue();
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				moveTag("FAMC", child, "FAMILY_CHILD");
				moveTag("ADOP", child, "FAMC", "ADOPTED_BY");
			}
			else{
				child.withTag("EVEN");
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
			}
		}
	}

}
