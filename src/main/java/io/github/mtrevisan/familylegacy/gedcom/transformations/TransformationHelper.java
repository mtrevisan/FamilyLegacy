package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
			currentContext.withTag(value);
		return currentContext;
	}

	public static List<GedcomNode> moveMultipleTag(final String value, final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(int i = 0; i < tags.length - 1; i ++){
			final String tag = tags[i];
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return Collections.emptyList();

			current = childrenWithTag.get(0);
		}

		final List<GedcomNode> currentContexts = current.getChildrenWithTag(tags[tags.length - 1]);
		for(final GedcomNode currentContext : currentContexts)
			currentContext.withTag(value);
		return currentContexts;
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
	public static GedcomNode extractPlaceStructure(final GedcomNode context, final String... tags){
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
	public static GedcomNode extractNote(final GedcomNode context){
		return (context.isEmpty() || context.getID() != null? context:
			GedcomNode.create("NOTE").withValue(context.getValueConcatenated()));
	}

	public static void transferNoteTo(final GedcomNode note, final GedcomNode root){
		if(note.getID() == null){
			//create a note in the root:
			note.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
			root.addChild(GedcomNode.create("NOTE")
				.withID(note.getID())
				.withValue(note.getValueConcatenated()));
			note.removeValue();
		}
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
			currentContext.withValueConcatenated(currentContext.getValue());
	}


	public static GedcomNode extractSubStructure(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(final String tag : tags){
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return GedcomNode.createEmpty();

			current = childrenWithTag.get(0);
		}
		return current;
	}

}
