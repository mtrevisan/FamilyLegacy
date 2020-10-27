package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;


final class TransformationHelper{

	private static final Set<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("ADDR", "CONT", "ADR1", "ADR2", "ADR3"));


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
	}

	public static void transferValue(final GedcomNode context, final String tag, final GedcomNode destination, final String destinationTag,
		final int destinationLevel){
		final GedcomNode componentContext = extractSubStructure(context, tag);
		if(!componentContext.isEmpty()){
			final GedcomNode component = GedcomNode.create(destinationLevel, destinationTag)
				.withValue(componentContext.getValue());
			destination.addChild(component);

			context.removeChild(componentContext);
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
			transferValue(placeContext, "CITY", place, "CITY", 1);
			transferValue(placeContext, "STAE", place, "STATE", 1);
			transferValue(placeContext, "POST", place, "POSTAL_CODE", 1);
			transferValue(placeContext, "CTRY", place, "COUNTRY", 1);

			transferValue(parentContext, "PHON", place, "PHONE", 1);
			transferValue(parentContext, "FAX", place, "FAX", 1);
			transferValue(parentContext, "EMAIL", place, "EMAIL", 1);
			transferValue(parentContext, "WWW", place, "WWW", 1);
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
