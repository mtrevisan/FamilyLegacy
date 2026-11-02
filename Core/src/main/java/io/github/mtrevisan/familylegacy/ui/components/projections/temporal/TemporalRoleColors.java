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
package io.github.mtrevisan.familylegacy.ui.components.projections.temporal;

import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.util.Locale;


/**
 * The color of a membership bar, by role: the same role always has the same color (the role is free text in the protocol, so the
 * color is taken from a palette by the role itself, without case: two roles can still share a color, the legend of the group views
 * shows the roles that are on the screen).
 */
public final class TemporalRoleColors{

	/** The color of a membership without a role. */
	public static final Color NO_ROLE = new Color(140, 140, 140);

	private static final Color[] PALETTE = {
		new Color(70, 120, 190),
		new Color(200, 100, 60),
		new Color(80, 150, 90),
		new Color(150, 90, 170),
		new Color(190, 160, 50),
		new Color(60, 150, 160),
		new Color(190, 80, 110),
		new Color(110, 110, 190),
		new Color(140, 110, 70),
		new Color(210, 130, 160),
		new Color(100, 170, 200),
		new Color(120, 130, 50)
	};


	private TemporalRoleColors(){}


	/**
	 * @param role	The role of the membership (can be {@code null}).
	 * @return	The color of the role: always the same one for the same role, whatever its case.
	 */
	public static Color of(final String role){
		if(StringUtils.isBlank(role))
			return NO_ROLE;

		final int index = Math.floorMod(role.strip().toLowerCase(Locale.ROOT).hashCode(), PALETTE.length);
		return PALETTE[index];
	}

}
