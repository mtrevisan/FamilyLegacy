package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class IndividualAttributeStructureTransformation implements Transformation{

	private enum EventTag{
		CAST("CASTE"),
		EDUC("EDUCATION"),
		IDNO("_IDNO"),
		NATI("ORIGIN"),
		OCCU("OCCUPATION"),
		PROP("POSSESSION"),
		RELI("RELIGION"),
		RESI("RESIDENCE"),
		SSN("SSN"),
		TITL("TITLE"),
		FACT("FACT");

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

	private enum EventTagPhysicalDescription{
		DSCR("PHYSICAL_DESCRIPTION");

		private final String code;

		static EventTagPhysicalDescription fromTag(final String tag){
			for(final EventTagPhysicalDescription et : values())
				if(et.toString().equals(tag))
					return et;
			return null;
		}

		static EventTagPhysicalDescription fromCode(final String code){
			for(final EventTagPhysicalDescription et : values())
				if(et.code.equals(code))
					return et;
			return null;
		}

		EventTagPhysicalDescription(final String code){
			this.code = code;
		}
	}

	private enum EventTagCount{
		NCHI("CHILDREN_COUNT"),
		NMR("MARRIAGES_COUNT");

		private final String code;

		static EventTagCount fromTag(final String tag){
			for(final EventTagCount et : values())
				if(et.toString().equals(tag))
					return et;
			return null;
		}

		static EventTagCount fromCode(final String code){
			for(final EventTagCount et : values())
				if(et.code.equals(code))
					return et;
			return null;
		}

		EventTagCount(final String code){
			this.code = code;
		}
	}

	private static final Transformation INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION = new IndividualEventDetailTransformation();
	private static final Transformation PHYSICAL_DESCRIPTION_DETAIL_TRANSFORMATION = new PhysicalDescriptionDetailTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			final EventTag et = EventTag.fromTag(tag);
			final EventTagPhysicalDescription etpd = EventTagPhysicalDescription.fromTag(tag);
			final EventTagCount etc = EventTagCount.fromTag(tag);
			if(et != null){
				child.withTag("ATTRIBUTE");
				final String value = child.getValue();
				if(value != null)
					child.addChild(GedcomNode.create("VALUE")
						.withValue(value));
				child.withValue(et.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(etpd != null){
				child.withTag("ATTRIBUTE");
				final String value = child.getValueConcatenated();
				if(value != null)
					child.addChild(GedcomNode.create("VALUE")
						.withValue(value));
				deleteTag(child, "CONC", "CONT");
				child.withValue(etpd.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
			else if(etc != null){
				child.withTag("ATTRIBUTE");
				final String value = child.getValueConcatenated();
				if(value != null)
					child.addChild(GedcomNode.create("VALUE")
						.withValue(value));
				child.withValue(etc.code);
				INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.to(child, root);
			}
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren())
			if("ATTRIBUTE".equals(child.getTag())){
				final String code = child.getValue();
				final EventTag et = EventTag.fromCode(code);
				final EventTagPhysicalDescription etpd = EventTagPhysicalDescription.fromCode(code);
				final EventTagCount etc = EventTagCount.fromCode(code);
				if(et != null){
					final GedcomNode value = extractSubStructure(child, "VALUE");
					child.withTag(et.toString());
					child.withValue(value.getValue());
					child.removeChild(value);
					INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				}
				else if(etpd != null){
					final GedcomNode value = extractSubStructure(child, "VALUE");
					child.withTag(etpd.toString());
					final String unstructuredValue = value.getValue();
					child.removeChild(value);
					PHYSICAL_DESCRIPTION_DETAIL_TRANSFORMATION.from(child, root);
					final String structuredValue = child.getValue();
					final StringJoiner sj = new StringJoiner(", ");
					if(unstructuredValue != null)
						sj.add(unstructuredValue);
					if(structuredValue != null)
						sj.add(structuredValue);
					if(sj.length() > 0)
						child.withValueConcatenated(sj.toString());
					INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				}
				else if(etc != null){
					final GedcomNode value = extractSubStructure(child, "VALUE");
					child.withTag(etc.toString());
					child.withValue(value.getValue());
					child.removeChild(value);
					INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
					moveTag("_QUAY", child, "CERTAINTY");
					moveTag("_CREDIBILITY", child, "CREDIBILITY");
				}
				else{
					child.withTag("FACT");
					final StringJoiner sj = new StringJoiner(" - ");
					if(child.getValue() != null)
						sj.add(child.getValue());
					for(final GedcomNode v : child.getChildrenWithTag("VALUE")){
						if(v.getValue() != null)
							sj.add(v.getValue());
						child.removeChild(v);
					}
					if(sj.length() > 0)
						child.withValue(sj.toString());
					INDIVIDUAL_EVENT_DETAIL_TRANSFORMATION.from(child, root);
				}
			}
	}

}
