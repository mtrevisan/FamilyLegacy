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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.groups.GroupMembershipFilter;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRoleColors;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * The legend of the role colors of a group view, that is also its filter by role: a box with the color and the name of the role, for
 * each role that is on the screen, in alphabetical order, and last the memberships without a role.
 * <p>
 * A box that is not checked hides the bars of its role (see {@link #filter(TemporalProjectionModel)}); a member or a group left with no
 * bar disappears with them. <em>Show all</em> checks them all again. The roles are free text: two spellings that differ only by case or by
 * blanks are one role, shown as the first met. The color is the one of {@link TemporalRoleColors}, the same that colors the bars. The
 * legend lists the roles of the whole view, hidden ones included, so that they can be shown again, and it is hidden when the view has no
 * membership.
 */
public final class GroupRoleLegend extends JPanel{

	/** The key of the memberships without a role. */
	public static final String NO_ROLE_KEY = "";

	private static final int SWATCH_SIZE = 12;
	private static final int COLUMNS = 8;
	private static final Color COLOR_HIDDEN_TEXT = new Color(0x909090);

	private final Set<String> hidden = new LinkedHashSet<>();
	private final Map<String, JCheckBox> boxes = new HashMap<>();
	private final JButton showAllButton = new JButton(I18N.t("legend.show_all"));

	/** The roles of the boxes now in the legend; {@code null} when the boxes are to be built again. */
	private List<Role> shown;
	private Runnable listener;


	/**
	 * A role of the legend.
	 *
	 * @param label  the text of the role, {@code null} for the memberships without a role
	 * @param color  the color of its bars
	 */
	public record Role(String label, Color color){

		/** @return	The key of the role: the same for the spellings that differ by case or blanks, {@link #NO_ROLE_KEY} for none. */
		public String key(){
			return GroupRoleLegend.key(label);
		}

	}


	public GroupRoleLegend(){
		super(new MigLayout("ins 2 6 2 6,gapx 14,gapy 2,wrap " + COLUMNS));

		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xC8C8C8)));
		setVisible(false);

		showAllButton.setMargin(new Insets(0, 6, 0, 6));
		showAllButton.addActionListener(e -> showAll());
		showAllButton.setVisible(false);
	}


	/**
	 * Tells what to do when a role is shown or hidden (the view has to be shown again, filtered).
	 *
	 * @param action	What to do.
	 * @return	This legend.
	 */
	public GroupRoleLegend withListener(final Runnable action){
		listener = action;

		return this;
	}

	/**
	 * Shows the roles of the memberships of the model. The roles hidden so far stay hidden; the boxes are built again only if the roles
	 * are not the ones already shown.
	 *
	 * @param model	The whole model of the view, not filtered ({@code null} shows nothing).
	 */
	public void update(final TemporalProjectionModel model){
		final List<Role> roles = roles(model);
		if(roles.equals(shown))
			return;

		shown = roles;
		boxes.clear();
		removeAll();
		if(!roles.isEmpty()){
			add(new JLabel(I18N.t("legend.roles") + ":"));
			for(final Role role : roles){
				final String text = (role.label() != null? role.label(): I18N.t("legend.role.none"));
				final JCheckBox box = new JCheckBox(text, new Swatch(role.color(), false), !hidden.contains(role.key()));
				box.setSelectedIcon(new Swatch(role.color(), true));
				box.setToolTipText(I18N.t("legend.role.toggle"));
				box.setFocusPainted(false);
				box.addActionListener(e -> setHidden(role.key(), !box.isSelected()));
				boxes.put(role.key(), box);
				add(box);
			}
			add(showAllButton);
		}
		synchronize();
		setVisible(!roles.isEmpty());
		revalidate();
		repaint();
	}

	/**
	 * Hides or shows the bars of a role, and tells the listener.
	 *
	 * @param roleKey	The key of the role (see {@link Role#key()}).
	 * @param hide		Whether to hide the role.
	 */
	public void setHidden(final String roleKey, final boolean hide){
		if(hide? !hidden.add(roleKey): !hidden.remove(roleKey))
			return;

		synchronize();
		notifyListener();
	}

	/** Shows the bars of all the roles again. */
	public void showAll(){
		if(hidden.isEmpty())
			return;

		hidden.clear();
		synchronize();
		notifyListener();
	}

	/** Forgets what was hidden, without telling the listener: for a new view, that has other roles. */
	public void reset(){
		hidden.clear();
		//the boxes are built again with the next update
		shown = null;
	}

	/** @return	The keys of the roles that are hidden. */
	public Set<String> hiddenRoles(){
		return Set.copyOf(hidden);
	}

	/** @return	The model without the bars of the hidden roles. */
	public TemporalProjectionModel filter(final TemporalProjectionModel model){
		return filter(model, hidden);
	}


	private void synchronize(){
		for(final Map.Entry<String, JCheckBox> entry : boxes.entrySet()){
			final boolean visible = !hidden.contains(entry.getKey());
			entry.getValue().setSelected(visible);
			entry.getValue().setForeground(visible? null: COLOR_HIDDEN_TEXT);
		}
		showAllButton.setVisible(!hidden.isEmpty());
	}

	private void notifyListener(){
		if(listener != null)
			listener.run();
	}


	/**
	 * @param role	The role of a membership (can be {@code null}).
	 * @return	Its key: stripped and lowercase, {@link #NO_ROLE_KEY} if it has no text.
	 */
	public static String key(final String role){
		final String stripped = StringUtils.strip(role);
		return (StringUtils.isEmpty(stripped)? NO_ROLE_KEY: stripped.toLowerCase(Locale.ROOT));
	}

	/**
	 * @param model	The model shown by the view (can be {@code null}).
	 * @return	The roles of the memberships of the model: the named ones alphabetically, without regard to case (a role written in more
	 * 	than one way is one, with the first spelling met), then the memberships without a role if there are some.
	 */
	public static List<Role> roles(final TemporalProjectionModel model){
		if(model == null)
			return List.of();

		final Map<String, String> named = new TreeMap<>();
		boolean withoutRole = false;
		for(final TemporalRow row : model.rows()){
			final TemporalTrack track = row.findTrack(TemporalTrackType.MEMBERSHIP);
			if(track == null)
				continue;

			for(final TemporalEntry entry : track.entries()){
				final String role = StringUtils.strip(entry.role());
				if(StringUtils.isEmpty(role))
					withoutRole = true;
				else
					named.putIfAbsent(role.toLowerCase(Locale.ROOT), role);
			}
		}

		final List<Role> roles = new ArrayList<>(named.size() + 1);
		for(final String role : named.values())
			roles.add(new Role(role, TemporalRoleColors.of(role)));
		if(withoutRole)
			roles.add(new Role(null, TemporalRoleColors.NO_ROLE));
		return roles;
	}

	/**
	 * Takes away the bars of some roles.
	 * <p>
	 * A row whose membership bars are all taken away disappears, unless it has other bars (the row of the group, or of the individual,
	 * with its events and attributes). The domain of the axis is computed again on what is left.
	 *
	 * @param model        the whole model of the view
	 * @param hiddenKeys   the keys of the roles to hide (see {@link #key(String)})
	 * @return	The model without them; the model itself if no bar is taken away.
	 */
	public static TemporalProjectionModel filter(final TemporalProjectionModel model, final Set<String> hiddenKeys){
		if(hiddenKeys.isEmpty())
			return model;

		return GroupMembershipFilter.apply(model, entry -> !hiddenKeys.contains(key(entry.role())));
	}


	/** A small square of the color of a role: filled if the role is shown, only the outline if it is hidden. */
	private record Swatch(Color color, boolean filled) implements Icon{

		@Override
		public void paintIcon(final Component c, final Graphics g, final int x, final int y){
			if(filled){
				g.setColor(color);
				g.fillRect(x, y, SWATCH_SIZE, SWATCH_SIZE);
			}
			g.setColor(color.darker());
			g.drawRect(x, y, SWATCH_SIZE - 1, SWATCH_SIZE - 1);
		}

		@Override
		public int getIconWidth(){
			return SWATCH_SIZE;
		}

		@Override
		public int getIconHeight(){
			return SWATCH_SIZE;
		}

	}

}
