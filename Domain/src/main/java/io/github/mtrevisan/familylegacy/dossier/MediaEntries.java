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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.IndividualMedia;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/** The rows of the section of the dossier that tells the media of an individual or of a group: the ones it cites, and its preferred image. */
final class MediaEntries{

	private MediaEntries(){}


	/**
	 * @param model      The model.
	 * @param individual The individual the dossier is about.
	 * @return A row for each media of the individual (the ones that the privacy policy hides are left out), that opens the editor of the
	 * 	individual on the media citation (the preferred image, if the media is only that). The row says whether the individual is tagged in the
	 * 	media (how many regions it has on it) and whether the media is its portrait.
	 */
	static List<DossierEntry> ofIndividual(final FLEFModel model, final FLEFRecord individual){
		return of(model, individual, true);
	}

	/**
	 * @param model The model.
	 * @param group The group the dossier is about.
	 * @return A row for each media of the group (the ones that the privacy policy hides are left out), that opens the editor of the group on the
	 * 	media citation (the preferred image, if the media is only that). The crop of a group is not a region (nobody is tagged by it), so the row
	 * 	only says whether the media is its preferred image.
	 */
	static List<DossierEntry> ofGroup(final FLEFModel model, final FLEFRecord group){
		return of(model, group, false);
	}

	private static List<DossierEntry> of(final FLEFModel model, final FLEFRecord owner, final boolean tagging){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final IndividualMedia item : MediaRegions.mediaOf(owner)){
			final FLEFRecord media = model.getRecordById(item.mediaId());
			if(media == null || PrivacyService.isHidden(model, media))
				continue;

			final String uri = MediaReader.extractUri(media);
			final String path = (uri != null? MediaPathDisplay.display(uri): StringUtils.EMPTY);
			final String caption = (item.citation() != null
				? StringUtils.trimToNull(FLEFRecordHelper.getChildValue(item.citation(), MediaCitationReader.TAG_CAPTION)): null);
			final String description = StringUtils.trimToNull(MediaReader.extractDescription(media));
			final String value = StringUtils.firstNonBlank(caption, description, path, item.mediaId());

			final List<String> details = new ArrayList<>(3);
			if(tagging && item.isTagged())
				details.add(I18N.tf("text.dossier.media.regions", item.regions().size()));
			if(item.portrait())
				details.add(I18N.t("text.dossier.media.portrait"));
			if(!value.equals(path) && !path.isEmpty())
				details.add(path);

			final DossierEntry entry = new DossierEntry(label(media, uri), value, String.join(" · ", details), StringUtils.EMPTY, owner,
				DossierEntry.Kind.NORMAL, null);
			entries.add(item.citation() != null? entry.withFocus(EditorTab.SOURCES, item.citation()): entry.withFocus(EditorTab.PROPERTIES, null));
		}
		return entries;
	}

	/** The kind of the media (a missing kind is inferred from the URI), as the editor of the media shows it. */
	private static String label(final FLEFRecord media, final String uri){
		final String kind = StringUtils.firstNonBlank(MediaReader.extractKind(media), MediaReader.inferKind(uri));
		return I18N.t("enum.media.kind." + kind, kind);
	}

}
