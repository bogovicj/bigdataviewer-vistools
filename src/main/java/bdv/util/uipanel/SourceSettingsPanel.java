package bdv.util.uipanel;

import bdv.tools.brightness.ColorIcon;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.PlaceHolderSource;
import bdv.viewer.Source;
import bdv.viewer.VisibilityAndGrouping;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

import static bdv.util.uipanel.ColorsAndIcons.BACKGROUND_COLOR;
import static bdv.util.uipanel.ColorsAndIcons.FOREGROUND_COLOR;
import static bdv.util.uipanel.ColorsAndIcons.notVisibleIcon;
import static bdv.util.uipanel.ColorsAndIcons.smallVisibilityIcon;
import static bdv.util.uipanel.ColorsAndIcons.visibleIcon;

/**
 * Select current source, change color/intensity, set visibility (active in fused mode).
 *
 * @author Tim-Oliver Buchholz
 * @author Tobias Pietzsch
 */
public class SourceSettingsPanel extends JPanel
{
	private final SourceIndexHelper sourceIndexHelper;

	private final SourceNameBimap sourceNameBimap;

	private final Map< Source< ? >, ConverterSetup > sourceToConverterSetup = new HashMap<>();

	/**
	 * Combobox displaying all current sources.
	 */
	private final JComboBox< Source< ? > > sourcesComboBox;

	/**
	 * Label representing the visibility state of the source.
	 */
	private final JCheckBox sourceVisibilityCheckbox;

	/**
	 * The information panel, showing information about the selected source.
	 */
	private final InformationPanel informationPanel;

	/**
	 * The min-max range slider-component.
	 */
	private final IntensitySlider intensitySlider;

	/**
	 * Single source mode checkbox.
	 */
	private final JCheckBox singleSourceModeCheckbox;

	public SourceSettingsPanel( final VisibilityAndGrouping visGro, final SourceIndexHelper sourceIndexHelper, final SourceNameBimap sourceNameBimap )
	{
		super( new MigLayout( "fillx", "[grow][][]", "[][]push[][]" ) );
		this.sourceIndexHelper = sourceIndexHelper;
		this.sourceNameBimap = sourceNameBimap;

		setBackground( BACKGROUND_COLOR );

		// source selection combobox
		sourcesComboBox = new JComboBox<>();
		sourcesComboBox.setMaximumSize( new Dimension( 270, 30 ) );
		sourcesComboBox.setRenderer( new SourceComboBoxRenderer() );
		sourcesComboBox.setBackground( BACKGROUND_COLOR );
		add( sourcesComboBox, "growx" );

		// source visibility icon (eye icon)
		sourceVisibilityCheckbox = new JCheckBox( notVisibleIcon );
		sourceVisibilityCheckbox.setSelectedIcon( visibleIcon );
		sourceVisibilityCheckbox.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		sourceVisibilityCheckbox.setToolTipText( "Show source in fused mode." );

		// color choser component
		final ColorButton colorButton = new ColorButton();
		add( colorButton );
		add( sourceVisibilityCheckbox, "wrap" );

		// add information panel
		informationPanel = new InformationPanel();
		add( informationPanel, "span, growx, wrap" );

		// single source mode checkbox to toggle between fused mode and single
		// source mode
		singleSourceModeCheckbox = new JCheckBox( "Single Source Mode" );
		singleSourceModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleSourceModeCheckbox.setToolTipText( "Display only the selected source." );

		// add range slider for intensity boundaries.
		intensitySlider = new IntensitySlider();
		final RangeSliderSpinnerPanel intensitySliderPanel = intensitySlider.getPanel();
		intensitySliderPanel.setPreferredSize( new Dimension( 20, 20 ) );
		add( intensitySliderPanel, "span, growx, wrap" );
		add( singleSourceModeCheckbox, "span, growx" );

		// -- set up listeners --

		// visibility checkbox --> visGro
		sourceVisibilityCheckbox.addActionListener( e -> {
			final Source< ? > source = sourceIndexHelper.getCurrentSource();
			final boolean active = !sourceIndexHelper.isSourceActive( source );
			visGro.setSourceActive( source, active );
		} );

		// TODO: converterSetup --> color button: Listener to ConverterSetup required?

		// single source checkbox --> visGro
		singleSourceModeCheckbox.addActionListener( e ->
				visGro.setFusedEnabled( !singleSourceModeCheckbox.isSelected() ) );

		// sourcesComboBox --> visGro
		sourcesComboBox.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				final Source< ? > source = ( Source< ? > ) sourcesComboBox.getSelectedItem();
				sourcesComboBox.setToolTipText( sourceNameBimap.getName( source ) );
				if ( source != null )
				{
					visGro.setCurrentSource( source );
				}
			}
		} );

		// visGro --> ui elements
		visGro.addUpdateListener( e -> {
			switch( e.id )
			{
			case VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED:
				final Source< ? > source = sourceIndexHelper.getCurrentSource();
				sourcesComboBox.setSelectedItem( source );
				intensitySlider.setSource( source );
				colorButton.setSource( source );
				if ( source instanceof PlaceHolderSource )
					informationPanel.setType( "N/A" );
				else
					informationPanel.setType( source.getType().getClass().getSimpleName() );
				updateSourceVisibilityButton();
				break;
			case VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED:
				sourcesComboBox.repaint();
				updateSourceVisibilityButton();
				break;
			case VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED:
				singleSourceModeCheckbox.setSelected( !visGro.isFusedEnabled() );
				break;
			case VisibilityAndGrouping.Event.NUM_SOURCES_CHANGED:
				// TODO: if bdv core would provide a Map<Source,ConverterSetup> or similar, this event could replace the sourceAdded() listener?
				break;
			}
		} );
	}

	public void sourceAdded( final Source< ? > source, final ConverterSetup converterSetup )
	{
		sourceToConverterSetup.put( source, converterSetup );
		intensitySlider.addSource( source, converterSetup );
		sourcesComboBox.addItem( source );
	}

	public void sourceRemoved( final Source< ? > source )
	{
		sourceToConverterSetup.remove( source );
		intensitySlider.removeSource( source );
		sourcesComboBox.removeItem( source );
	}

	/**
	 * Sets whether or not this panel and its children are enabled.
	 *
	 * @param enabled
	 *        {@code true} if this panel should be enabled, {@code false} if it should be disabled.
	 */
	@Override
	public void setEnabled( final boolean enabled )
	{
		sourcesComboBox.setEnabled( enabled );
		singleSourceModeCheckbox.setEnabled( enabled );
		intensitySlider.setEnabled( enabled ); // TODO: What is the intended behaviour? Should the whole panel be disabled or is it okay to change activity and ranges?
		informationPanel.setEnabled( enabled ); // TODO: Same as above. I think switching here looks better.
		super.setEnabled( enabled );
	}

	private void updateSourceVisibilityButton()
	{
		sourceVisibilityCheckbox.setSelected( sourceIndexHelper.isSourceActive( sourceIndexHelper.getCurrentSource() ) );
	}

	/**
	 * A button to change the color of a source.
	 */
	class ColorButton extends JButton
	{
		// ConverterSetup which this ColorButton is currently manipulating
		private ConverterSetup setup;

		public ColorButton()
		{
			super();
			setPreferredSize( new Dimension( 15, 15 ) );
			addActionListener( e -> {
				Color newColor = JColorChooser.showDialog( null, "Select Source Color", getColor( setup ) );
				if ( newColor != null )
				{
					updateButton( newColor ); // TODO: ideally should not be necessary
					setColor( setup, newColor );
				}
			} );
			setSource( null );
		}

		public void setSource( Source< ? > source )
		{
			setup = sourceToConverterSetup.get( source );
			updateButton( getColor( setup ) );
		}

		private void updateButton( final Color color )
		{
			setIcon( colorIcon( color ) );
			setEnabled( color != null );
		}

		private ColorIcon colorIcon( Color color )
		{
			return new ColorIcon( color, 24, 16, 5, 5 );
		}
	}

	private static Color getColor( final ConverterSetup setup )
	{
		return ( setup != null && setup.supportsColor() ) ? new Color( setup.getColor().get() ) : null;
	}

	private static void setColor( final ConverterSetup setup, final Color color )
	{
		setup.setColor( new ARGBType( color.getRGB() | 0xff000000 ) );
	}

	/**
	 * A combobox renderer displaying the visibility state of the sources.
	 */
	class SourceComboBoxRenderer extends JLabel implements ListCellRenderer< Source< ? > >
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends Source< ? > > list, final Source< ? > value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value != null )
			{
				final String uniqueName = sourceNameBimap.getName( value );
				setText( uniqueName );
				setToolTipText( uniqueName );
				final boolean visible = sourceIndexHelper.isSourceActive( value );
				setIcon( smallVisibilityIcon( visible ) );
			}
			else
				setIcon( null );

			setForeground( isSelected ? Color.gray : FOREGROUND_COLOR );
			return this;
		}
	}
}
