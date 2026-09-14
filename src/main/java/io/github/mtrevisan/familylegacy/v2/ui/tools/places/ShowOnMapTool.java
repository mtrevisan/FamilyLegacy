package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * Lets the user pick a place and opens it on a map.
 * <p>
 * When the place has coordinates, the map is opened directly on the
 * point. Otherwise, a search query on the place name is used. The map
 * opens in the system browser through {@link Desktop#browse(URI)}.
 */
public final class ShowOnMapTool implements ToolOperation{

	private static final String MAP_BASE_URL = "https://www.openstreetmap.org/";


	@Override
	public String getName(){
		return "Show on Map…";
	}

	@Override
	public void run(final ToolContext context){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			context.owner(), context.model(),
			(record, handler) -> chosen[0] = record,
			PlaceHandler.class);
		dialog.setVisible(true);

		if(chosen[0] == null)
			return;

		final String url = buildUrl(chosen[0]);
		if(url == null){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to build a map URL for this place.",
				"Show on Map", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if(!Desktop.isDesktopSupported()
			|| !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
			JOptionPane.showMessageDialog(context.owner(),
				"Opening the map is not supported on this platform.",
				"Show on Map", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try{
			Desktop.getDesktop().browse(new URI(url));
		}
		catch(final URISyntaxException | IOException e){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to open the map:\n" + e.getMessage(),
				"Show on Map", JOptionPane.ERROR_MESSAGE);
		}
	}


	/**
	 * Builds the map URL. When the place has coordinates of the form
	 * "latitude, longitude" (or "lat, lon" with any separator), the URL
	 * points at that exact position. Otherwise, the URL points at a
	 * search query on the place name.
	 */
	private static String buildUrl(final FLEFRecord place){
		final String coords = PlaceHelper.coordinates(place);
		if(coords != null){
			final String[] parts = coords.split("[,\\s]+");
			if(parts.length >= 2){
				try{
					final double lat = Double.parseDouble(parts[0]);
					final double lon = Double.parseDouble(parts[1]);
					return MAP_BASE_URL + "?mlat=" + lat + "&mlon=" + lon
						+ "#map=14/" + lat + "/" + lon;
				}
				catch(final NumberFormatException ignored){
					// Fall through to the search by name.
				}
			}
		}
		final String name = PlaceHelper.displayName(place);
		if(name == null || name.isBlank())
			return null;
		return MAP_BASE_URL + "search?query="
			+ URLEncoder.encode(name, StandardCharsets.UTF_8);
	}

}
