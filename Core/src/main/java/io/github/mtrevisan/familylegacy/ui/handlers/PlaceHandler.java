/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.awt.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Handler for PLACE records.
 */
public class PlaceHandler extends AbstractRecordTypeHandler<PlaceRecordDialog>{

	public static final String TYPE = "PLACE";
	public static final String ID_PREFIX = "P";


	private static final class SingletonHelper{
		private static final PlaceHandler INSTANCE = new PlaceHandler();
	}


	public static PlaceHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist_record.place");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String id = record.getId();
		if(id == null)
			return PlaceCitationHandler.getInstance()
				.getDisplayText(record, model);

		final String type = PlaceReader.extractType(record);

		final String primaryName = PlaceReader.extractPrimaryName(record);
		if(primaryName == null)
			return "[" + id + "]";

		// Reconstruct the full hierarchical path by walking up parent relationships
		final List<String> pathNames = new ArrayList<>();
		pathNames.add(primaryName);

		if(model != null){
			String currentId = id;
			final Set<String> visited = new HashSet<>();
			visited.add(currentId);

			while(true){
				String parentId = null;
				String parentName = null;

				final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
				for(final FLEFRecord placeRelationship : placeRelationships){
					final String subjId = placeRelationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, TYPE);
					if(currentId.equals(subjId)){
						final String objId = placeRelationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, TYPE);
						if(objId != null && visited.add(objId)){
							final FLEFRecord parentRecord = model.getRecordById(objId);
							if(parentRecord != null){
								parentName = PlaceReader.extractPrimaryName(parentRecord);

								parentId = objId;

								break;
							}
						}
					}
				}

				if(parentName == null)
					break;

				pathNames.add(parentName);
				currentId = parentId;
			}
		}

		return String.join(", ", pathNames) + " [" + id + "]";
	}

	@Override
	public PlaceRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return PlaceRecordDialog.createNew(parent, model);
	}

	@Override
	public PlaceRecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return PlaceRecordDialog.createEdit(parent, model, record);
	}

}
