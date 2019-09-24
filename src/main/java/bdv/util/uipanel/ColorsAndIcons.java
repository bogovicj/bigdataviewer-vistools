package bdv.util.uipanel;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class ColorsAndIcons
{
	/**
	 * The UI foreground color.
	 */
	public static final Color FOREGROUND_COLOR = Color.darkGray;

	/**
	 * The UI background color.
	 */
	public static final Color BACKGROUND_COLOR = Color.white;

	/**
	 * Eye icon normal size.
	 */
	public static final ImageIcon visibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible.png" ), "Visible" );

	/**
	 * Crossed eye icon normal size.
	 */
	public static final ImageIcon notVisibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible.png" ), "Not Visible" );

	/**
	 * Eye icon small.
	 */
	public static final ImageIcon visibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible_small.png" ), "Visible" );

	/**
	 * Crossed eye icon small.
	 */
	public static final ImageIcon notVisibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible_small.png" ), "Not Visible" );

	public static Icon smallVisibilityIcon( final boolean active )
	{
		return active ? visibleIconSmall : notVisibleIconSmall;
	}
}
