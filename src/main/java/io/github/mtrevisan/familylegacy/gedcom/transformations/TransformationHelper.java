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

	public static void addNode(final GedcomNode node, final GedcomNode context, final String... tags){
		final GedcomNode currentContext = extractSubStructure(context, tags);

		if(!currentContext.isEmpty())
			currentContext.addChild(node);
	}

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

	public static void transferValues(final GedcomNode context, final String tag, final GedcomNode destination, final String destinationTag,
			final int destinationLevel){
		final List<GedcomNode> componentContext = context.getChildrenWithTag(tag);
		for(final GedcomNode child : componentContext){
			final GedcomNode component = GedcomNode.create(destinationLevel, destinationTag)
				.withValue(child.getValue());
			destination.addChild(component);

			context.removeChild(child);
		}
	}


	/** NOTE: remember to set xref! */
	public static GedcomNode extractPlace(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode placeContext = extractSubStructure(parentContext, "ADDR");
		parentContext.removeChild(placeContext);

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

			place = GedcomNode.create(0, "PLACE");
			if(value != null && !value.isEmpty())
				place.addChild(GedcomNode.create(1, "STREET")
					.withValue(value));
			transferValues(placeContext, "CITY", place, "CITY", 1);
			transferValues(placeContext, "STAE", place, "STATE", 1);
			transferValues(placeContext, "POST", place, "POSTAL_CODE", 1);
			transferValues(placeContext, "CTRY", place, "COUNTRY", 1);

			transferValues(parentContext, "PHON", place, "PHONE", 1);
			transferValues(parentContext, "FAX", place, "FAX", 1);
			transferValues(parentContext, "EMAIL", place, "EMAIL", 1);
			transferValues(parentContext, "WWW", place, "WWW", 1);
		}
		return place;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractNote(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode noteContext = extractSubStructure(parentContext, "NOTE");
		parentContext.removeChild(noteContext);

		GedcomNode place = GedcomNode.createEmpty();
		if(!noteContext.isEmpty()){
			final StringJoiner street = new StringJoiner(" - ");
			String value = noteContext.getValue();
			final Iterator<GedcomNode> itr = noteContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if(ADDRESS_TAGS.contains(child.getTag())){
					value = child.getValue();
					if(value != null && ! value.isEmpty())
						street.add(value);

					itr.remove();
				}
			}
			if(street.length() > 0)
				value = street.toString();

			place = GedcomNode.create(0, "PLACE");
			if(value != null && !value.isEmpty()){
				final GedcomNode component = GedcomNode.create(1, "STREET")
					.withValue(value);
				place.addChild(component);
			}
			transferValues(noteContext, "CITY", place, "CITY", 1);
			transferValues(noteContext, "STAE", place, "STATE", 1);
			transferValues(noteContext, "POST", place, "POSTAL_CODE", 1);
			transferValues(noteContext, "CTRY", place, "COUNTRY", 1);

			transferValues(parentContext, "PHON", place, "PHONE", 1);
			transferValues(parentContext, "FAX", place, "FAX", 1);
			transferValues(parentContext, "EMAIL", place, "EMAIL", 1);
			transferValues(parentContext, "WWW", place, "WWW", 1);
		}
		return place;
	}

	public static void mergeNote(final GedcomNode context, final String... keys){
		final GedcomNode currentContext = extractSubStructure(context, keys);

		if(!currentContext.isEmpty()){
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
			boolean found = false;
			for(final GedcomNode child : current.getChildren())
				if(tag.equals(child.getTag())){
					found = true;
					current = child;
					break;
				}
			if(!found){
				current = GedcomNode.createEmpty();
				break;
			}
		}
		return current;
	}

}
