package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


final class TransformationHelper{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("ADDR", "CONT", "ADR1", "ADR2", "ADR3"));


	private TransformationHelper(){}

//	public static void addNode(final GedcomNode node, final GedcomNode context, final String... tags){
//		final GedcomNode currentContext = extractSubStructure(context, tags);
//
//		if(!currentContext.isEmpty())
//			currentContext.addChild(node);
//	}

	public static GedcomNode moveTag(final String value, final GedcomNode context, final String... tags){
		final GedcomNode currentContext = extractSubStructure(context, tags);

		if(!currentContext.isEmpty())
			currentContext.setTag(value);
		return currentContext;
	}

	public static void deleteTag(final GedcomNode context, final String... tags){
		final String lastTag = tags[tags.length - 1];
		final String[] firstTags = ArrayUtils.remove(tags, tags.length - 1);
		final GedcomNode currentContext = extractSubStructure(context, firstTags);

		if(!currentContext.isEmpty())
			currentContext.getChildren()
				.removeIf(gedcomNode -> lastTag.equals(gedcomNode.getTag()));
		if(currentContext.getChildren().isEmpty())
			currentContext.removeChildren();
	}

	public static void transferValues(final GedcomNode context, final String tag, final GedcomNode destination, final String destinationTag){
		final List<GedcomNode> componentContext = context.getChildrenWithTag(tag);
		for(final GedcomNode child : componentContext){
			destination.addChild(GedcomNode.create(destinationTag)
				.withValue(child.getValue()));

			context.removeChild(child);
		}
	}


	/** NOTE: remember to set xref! */
	public static GedcomNode extractPlace(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode placeContext = extractSubStructure(parentContext, "ADDR");

		GedcomNode place = GedcomNode.createEmpty();
		if(!placeContext.isEmpty()){
			final StringJoiner street = new StringJoiner(" - ");
			String value = placeContext.getValue();
			final Iterator<GedcomNode> itr = placeContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if(ADDRESS_TAGS.contains(child.getTag())){
					final String component = child.getValue();
					if(component != null && ! component.isEmpty())
						street.add(component);

					itr.remove();
				}
			}
			if(street.length() > 0)
				value = street.toString();

			place = GedcomNode.create("PLACE");
			if(value != null && !value.isEmpty())
				place.addChild(GedcomNode.create("STREET")
					.withValue(value));
			transferValues(placeContext, "CITY", place, "CITY");
			transferValues(placeContext, "STAE", place, "STATE");
			transferValues(placeContext, "POST", place, "POSTAL_CODE");
			transferValues(placeContext, "CTRY", place, "COUNTRY");

			transferValues(parentContext, "PHON", place, "PHONE");
			transferValues(parentContext, "FAX", place, "FAX");
			transferValues(parentContext, "EMAIL", place, "EMAIL");
			transferValues(parentContext, "WWW", place, "WWW");

			parentContext.removeChild(placeContext);
		}
		return place;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractNote(final GedcomNode context, final GedcomNode parent){
		GedcomNode note = GedcomNode.createEmpty();
		if(!context.isEmpty() && context.getID() == null){
			note = GedcomNode.create("NOTE")
				.withValue(context.getValueConcatenated());

			parent.removeChild(context);
		}
		return note;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractPlainNote(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode noteContext = extractSubStructure(parentContext, "NOTE");

		GedcomNode note = GedcomNode.createEmpty();
		if(!noteContext.isEmpty()){
			note = GedcomNode.create("NOTE")
				.withValue(noteContext.getValueConcatenated());

			parentContext.removeChild(noteContext);
		}
		return note;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractSource(final GedcomNode context, final GedcomNode parent){
		GedcomNode note = GedcomNode.createEmpty();
		if(!context.isEmpty()){
			context.setTag("SOURCE");
			final List<GedcomNode> children = context.removeChildren();
			final String sourceID = context.getID();
			if(sourceID != null){
				final GedcomNode sourcePage = extractSubStructure(context, "PAGE");
				final GedcomNode sourceEvent = extractSubStructure(context, "EVEN");
/*
			+2 EVEN <EVENT_TYPE_CITED_FROM>    {0:1}
				+3 ROLE <ROLE_IN_EVENT>    {0:1}
			+2 DATA    {0:1}
				+3 DATE <ENTRY_RECORDING_DATE>    {0:1}
				+3 TEXT <TEXT_FROM_SOURCE>    {0:M}
					+4 [CONC|CONT] <TEXT_FROM_SOURCE>    {0:M}
*/
			}
			else{
				final List<GedcomNode> sourceTexts = context.getChildrenWithTag("TEXT");
				for(final GedcomNode sourceText : sourceTexts){
					final String text = sourceText.getValueConcatenated();
					//TODO
				}

				note = GedcomNode.create("SOUR")
					.withValue(context.getValueConcatenated());
			}
/*
			+2 <<MULTIMEDIA_LINK>>    {0:M}
			+2 <<NOTE_STRUCTURE>>    {0:M}
			+2 QUAY <CERTAINTY_ASSESSMENT>    {0:1}
*/
			context.clear();
			context.withID(sourceID);
		}
		return note;
	}

	public static void mergeNote(final GedcomNode context, final String... keys){
		final GedcomNode currentContext = extractSubStructure(context, keys);

		if(!currentContext.isEmpty()){
			currentContext.withValue(currentContext.getValueConcatenated());
			deleteTag(currentContext, "CONC", "CONT");

			final StringBuilder sb = new StringBuilder(currentContext.getValue());
			final Iterator<GedcomNode> itr = currentContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if("CONC".equals(child.getTag())){
					sb.append(child.getValue());
					itr.remove();
				}
				else if("CONT".equals(child.getTag())){
					sb.append("\\n");
					sb.append(child.getValue());
					itr.remove();
				}
			}
			context.withValue(sb.toString());
		}
	}

	public static void splitNote(final GedcomNode context, final String... keys){
		final GedcomNode currentContext = extractSubStructure(context, keys);

		if(!currentContext.isEmpty())
			currentContext.setValueConcatenated(currentContext.getValue());
	}


	public static GedcomNode extractSubStructure(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(final String tag : tags){
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.isEmpty() || childrenWithTag.size() != 1)
				return GedcomNode.createEmpty();

			current = childrenWithTag.get(0);
		}
		return current;
	}

}
