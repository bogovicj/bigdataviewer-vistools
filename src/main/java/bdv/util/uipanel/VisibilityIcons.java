package bdv.util.uipanel;

import javax.swing.ImageIcon;

public final class VisibilityIcons
{
	public static ImageIcon big( boolean isVisible )
	{
		return isVisible ? instance().visibleIcon : instance().notVisibleIcon;
	}

	public static ImageIcon small( boolean isVisible )
	{
		return isVisible ? instance().visibleIconSmall : instance().notVisibleIconSmall;
	}

	/**
	 * Eye icon normal size.
	 */
	private final ImageIcon visibleIcon;

	/**
	 * Crossed eye icon normal size.
	 */
	private final ImageIcon notVisibleIcon;

	/**
	 * Eye icon small.
	 */
	private final ImageIcon visibleIconSmall;

	/**
	 * Crossed eye icon small.
	 */
	private final ImageIcon notVisibleIconSmall;

	private VisibilityIcons()
	{
		visibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible.png" ), "Visible" );
		notVisibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible.png" ), "Not Visible" );
		visibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible_small.png" ), "Visible" );
		notVisibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible_small.png" ), "Not Visible" );
	}

	private static volatile VisibilityIcons instance;

	private static VisibilityIcons instance()
	{
		if ( instance == null )
		{
			synchronized ( VisibilityIcons.class )
			{
				if ( instance == null )
					instance = new VisibilityIcons();
			}
		}
		return instance;
	}
}
