package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for Source records: title, author, publisher, media type, and place.
 */
public class SourceFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_TITLE = "title";
	static final String FILTER_KEY_AUTHOR = "author";
	static final String FILTER_KEY_PUBLISHER = "publisher";
	static final String FILTER_KEY_MEDIA_TYPE = "mediaType";
	static final String FILTER_KEY_PLACE = "place";


	private final JTextField titleField = new JTextField(20);
	private final JTextField authorField = new JTextField(20);
	private final JTextField publisherField = new JTextField(20);
	private final JComboBox<String> mediaTypeCombo = new JComboBox<>(new String[]{
		"Any",
		"audio", "book", "card", "electronic", "fiche", "film",
		"magazine", "manuscript", "map", "newspaper", "photo", "tombstone", "video"
	});
	private final JTextField placeField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public SourceFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Source Filters"));

		add(new JLabel("Title:"));
		add(titleField, "growx");
		add(new JLabel("Author:"));
		add(authorField, "growx");
		add(new JLabel("Publisher:"));
		add(publisherField, "growx");
		add(new JLabel("Media Type:"));
		add(mediaTypeCombo, "growx");
		add(new JLabel("Place:"));
		add(placeField, "growx");
	}

	private void setupListeners(){
		mediaTypeCombo.addActionListener(e -> fireChanged());

		final DocumentListener docListener = new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				fireChanged();
			}
		};

		titleField.getDocument()
			.addDocumentListener(docListener);
		authorField.getDocument()
			.addDocumentListener(docListener);
		publisherField.getDocument()
			.addDocumentListener(docListener);
		placeField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_TITLE, getTitle());
		filters.put(FILTER_KEY_AUTHOR, getAuthor());
		filters.put(FILTER_KEY_PUBLISHER, getPublisher());
		filters.put(FILTER_KEY_MEDIA_TYPE, getMediaType());
		filters.put(FILTER_KEY_PLACE, getPlace());
		return filters;
	}

	public String getTitle(){
		return titleField.getText().trim();
	}

	public String getAuthor(){
		return authorField.getText().trim();
	}

	public String getPublisher(){
		return publisherField.getText().trim();
	}

	public String getMediaType(){
		return (mediaTypeCombo.getSelectedIndex() == 0? null: (String)mediaTypeCombo.getSelectedItem());
	}

	public String getPlace(){
		return placeField.getText().trim();
	}

}
