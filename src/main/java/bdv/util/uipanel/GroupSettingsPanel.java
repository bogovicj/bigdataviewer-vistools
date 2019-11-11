package bdv.util.uipanel;

import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import net.miginfocom.swing.MigLayout;

import static bdv.util.uipanel.ColorsAndIcons.BACKGROUND_COLOR;
import static bdv.util.uipanel.ColorsAndIcons.FOREGROUND_COLOR;
import static bdv.util.uipanel.ColorsAndIcons.smallVisibilityIcon;

/**
 * Select and edit source groups.
 *
 * @author Tim-Oliver Buchholz
 * @author Tobias Pietzsch
 */
public class GroupSettingsPanel extends JPanel
{
	private final ViewerPanel viewerPanel;

	private final VisibilityAndGrouping visGro;

	private final SourceIndexHelper sourceIndexHelper;

	private final SourceNameBimap sourceNameBimap;

	/**
	 * These groups cannot be deleted (through this panel).
	 */
	private final Set< SourceGroup > protectedGroups;

	/**
	 * Item to add new groups.
	 */
	private final SourceGroup NEW_GROUP = new SourceGroup( "<New Group>" );

	/**
	 * Combobox displaying all groups with an option to create new groups.
	 */
	private final JComboBox< SourceGroup > groupsComboBox;

	/**
	 * Label representing the visibility state of the group.
	 */
	private final JCheckBox groupVisibilityCheckbox;

	/**
	 * Single groupp mode checkbox.
	 */
	private final JCheckBox singleGroupModeCheckbox;

	/**
	 * Remove the selected group button.
	 */
	private final JButton removeGroup;

	/**
	 * Splitpane holding the selected sources and remaining (not selected)
	 * sources of a group.
	 */
	private final JSplitPane selection;

	/**
	 * Sources which are part of the selected group.
	 */
	private final JList< SourceEntry > selectedSourcesList;

	private final SourceListModel remainingSourceListModel;

	/**
	 * Sources which are not part of the selected group.
	 */
	private final JList< SourceEntry > remainingSourcesList;

	private final SourceListModel selectedSourceListModel;

	/**
	 * @param protectedGroups
	 * 		groups that cannot be deleted (i.e., the "All" group).
	 */
	public GroupSettingsPanel(
			final ViewerPanel viewerPanel,
			final VisibilityAndGrouping visGro,
			final SourceIndexHelper sourceIndexHelper,
			final SourceNameBimap sourceNameBimap,
			Set< SourceGroup > protectedGroups )
	{
		super( new MigLayout( "fillx", "[grow][][]", "" ) );

		this.viewerPanel = viewerPanel;
		this.visGro = visGro;
		this.sourceIndexHelper = sourceIndexHelper;
		this.sourceNameBimap = sourceNameBimap;
		this.protectedGroups = new HashSet<>( protectedGroups );

		setBackground( BACKGROUND_COLOR );

		groupsComboBox = new JComboBox<>();
		groupsComboBox.setMaximumSize( new Dimension( 269, 30 ) );
		groupsComboBox.setRenderer( new GroupComboBoxRenderer() );
		groupsComboBox.setBackground( BACKGROUND_COLOR );
		groupsComboBox.setForeground( FOREGROUND_COLOR );

		// remove group button
		removeGroup = new JButton( "-" );
		removeGroup.setForeground( FOREGROUND_COLOR );
		removeGroup.setBackground( BACKGROUND_COLOR );

		// selected sources list holds all sources which are part of the selected groupp
		selectedSourcesList = new JList<>();
		selectedSourcesList.setBackground( BACKGROUND_COLOR );
		selectedSourcesList.setBorder( null );

		selectedSourceListModel = new SourceListModel( this.sourceNameBimap );
		selectedSourcesList.setModel( selectedSourceListModel );
		selectedSourcesList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( final ListSelectionEvent e )
			{
				if ( !e.getValueIsAdjusting() && selectedSourcesList.getSelectedIndex() >= 0 )
				{
					final SourceEntry selectedSource = selectedSourceListModel.getElementAt( selectedSourcesList.getSelectedIndex() );
					selectedSourceListModel.remove( selectedSource );
					remainingSourceListModel.add( selectedSource );

					sourceIndexHelper.removeSourceFromGroup( selectedSource.source, sourceIndexHelper.getCurrentGroup() );
					selectedSourcesList.clearSelection();
				}
			}
		} );

		// remaining sources list holds all sources which are not part of the selected group
		remainingSourcesList = new JList<>();
		remainingSourcesList.setBackground( BACKGROUND_COLOR );
		remainingSourcesList.setBorder( null );

		remainingSourceListModel = new SourceListModel( this.sourceNameBimap );
		remainingSourcesList.setModel( remainingSourceListModel );
		remainingSourcesList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( final ListSelectionEvent e )
			{
				if ( !e.getValueIsAdjusting() && remainingSourcesList.getSelectedIndex() >= 0 )
				{
					final SourceEntry selectedSource = remainingSourceListModel.getElementAt( remainingSourcesList.getSelectedIndex() );
					remainingSourceListModel.remove( selectedSource );
					selectedSourceListModel.add( selectedSource );

					sourceIndexHelper.addSourceToGroup( selectedSource.source, sourceIndexHelper.getCurrentGroup() );
					remainingSourcesList.clearSelection();
				}
			}
		} );

		// the split pane holding selected and remaining sources
		selection = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		selection.setPreferredSize( new Dimension( selection.getPreferredSize().width, 150 ) );
		selection.setUI( new BasicSplitPaneUI()
		{
			@Override
			public BasicSplitPaneDivider createDefaultDivider()
			{
				return new BasicSplitPaneDivider( this )
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void paint( final Graphics g )
					{
						g.setColor( BACKGROUND_COLOR );
						g.fillRect( 0, 0, getSize().width, getSize().height );
						super.paint( g );
					}
				};
			}
		} );
		selection.setDividerLocation( 70 );
		selection.setBackground( BACKGROUND_COLOR );
		selection.setForeground( FOREGROUND_COLOR );
		selection.setBorder( null );
		final JScrollPane scrollPaneTop = new JScrollPane( selectedSourcesList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scrollPaneTop.getVerticalScrollBar().setUI( new WhiteScrollBarUI() );
		scrollPaneTop.getHorizontalScrollBar().setUI( new WhiteScrollBarUI() );
		selection.setTopComponent( scrollPaneTop );
		final JScrollPane scrollPaneBottom = new JScrollPane( remainingSourcesList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scrollPaneBottom.getVerticalScrollBar().setUI( new WhiteScrollBarUI() );
		scrollPaneBottom.getHorizontalScrollBar().setUI( new WhiteScrollBarUI() );
		selection.setBottomComponent( scrollPaneBottom );

		// label displaying the visibility state of the current group (eye icon)
		groupVisibilityCheckbox = new JCheckBox( ColorsAndIcons.notVisibleIcon );
		groupVisibilityCheckbox.setSelectedIcon( ColorsAndIcons.visibleIcon );
		groupVisibilityCheckbox.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		groupVisibilityCheckbox.setToolTipText( "Show group in fused-group mode." );

		// checkbox to toggle between fused group mode and single group mode
		singleGroupModeCheckbox = new JCheckBox( "Single Group Mode" );
		singleGroupModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleGroupModeCheckbox.setToolTipText( "Display only the currently selected group." );

		add( groupsComboBox, "growx" );
		add( groupVisibilityCheckbox );
		add( removeGroup, "growx, wrap" );
		add( selection, "span, growx, wrap" );
		add( singleGroupModeCheckbox, "span, growx" );

		// -- initialize state for current group --

		updateGroupsComboBox();
		updateGroupVisibilityButton();
		updateGroupSourceComponent();
		groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );

		// -- set up listeners --

		// visibility checkbox --> visGro
		groupVisibilityCheckbox.addActionListener( e -> {
			final SourceGroup group = sourceIndexHelper.getCurrentGroup();
			sourceIndexHelper.setGroupActive( group, !group.isActive() );
		} );

		// remove group --> viewerPanel
		removeGroup.addActionListener( e -> viewerPanel.removeGroup( sourceIndexHelper.getCurrentGroup() ) );

		// single group checkbox --> visGro
		singleGroupModeCheckbox.addActionListener( e ->
				visGro.setFusedEnabled( !singleGroupModeCheckbox.isSelected() ) );

		// groupsComboBox --> visGro
		// Also handles new group creation.
		groupsComboBox.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				final SourceGroup selected = ( SourceGroup ) e.getItem();
				if ( selected.equals( NEW_GROUP ) )
				{
					/*
					 * When the groupsComboBox is rebuild and NEW_GROUP is added as the first item,
					 * then NEW_GROUP is automatically selected. Ignore this...
					 */
					if ( groupsComboBox.getItemCount() <= 1 )
						return;

					final String newGroupName = JOptionPane.showInputDialog( this, "New Group Name:" );
					if ( newGroupName != null && !newGroupName.isEmpty() )
					{
						final Set< String > groupNames = visGro.getSourceGroups().stream().map( SourceGroup::getName ).collect( Collectors.toSet() );
						if ( groupNames.contains( newGroupName ) )
						{
							JOptionPane.showMessageDialog( this, "This group already exists." );
							groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );
						}
						else
						{
							final SourceGroup newGroup = new SourceGroup( newGroupName );
							viewerPanel.addGroup( newGroup );
							sourceIndexHelper.setCurrentGroup( newGroup );
						}
					}
				}
				else
					sourceIndexHelper.setCurrentGroup( selected );
			}
		} );

		// visGro --> ui elements
		visGro.addUpdateListener( e -> {
			switch ( e.id )
			{
			case VisibilityAndGrouping.Event.CURRENT_GROUP_CHANGED:
				final SourceGroup group = sourceIndexHelper.getCurrentGroup();
				groupsComboBox.setSelectedItem( group );
				removeGroup.setEnabled( !this.protectedGroups.contains( group ) );
				updateGroupSourceComponent();
				updateGroupVisibilityButton();
				break;
			case VisibilityAndGrouping.Event.GROUP_ACTIVITY_CHANGED:
				groupsComboBox.repaint();
				updateGroupVisibilityButton();
				break;
			case VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED:
				singleGroupModeCheckbox.setSelected( !visGro.isFusedEnabled() );
				break;
			case VisibilityAndGrouping.Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
				updateGroupSourceComponent();
				break;
			case VisibilityAndGrouping.Event.GROUP_NAME_CHANGED:
				groupsComboBox.repaint();
				break;
			case VisibilityAndGrouping.Event.NUM_GROUPS_CHANGED:
				updateGroupsComboBox();
				break;
			}
		} );
	}

	private void updateGroupsComboBox()
	{
		groupsComboBox.removeAllItems();
		groupsComboBox.addItem( NEW_GROUP ); // entry which opens the add-group dialog
		visGro.getSourceGroups().forEach( groupsComboBox::addItem );
		groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );
	}

	private void updateGroupVisibilityButton()
	{
		groupVisibilityCheckbox.setSelected( sourceIndexHelper.getCurrentGroup().isActive() );
	}

	/**
	 * TODO
	 * Rebuild selectedSources and remainingSources panel
	 */
	private void updateGroupSourceComponent()
	{
		selectedSourceListModel.removeAll();
		remainingSourceListModel.removeAll();

		final SourceGroup group = sourceIndexHelper.getCurrentGroup();
		viewerPanel.getState().getSources().forEach( sourceState -> {
			final Source< ? > source = sourceState.getSpimSource();
			if ( sourceIndexHelper.contains( group, source ) )
				selectedSourceListModel.add( source );
			else
				remainingSourceListModel.add( source );
		} );
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
		groupsComboBox.setEnabled( enabled );
		singleGroupModeCheckbox.setEnabled( enabled );
		selection.setEnabled( enabled );
		selectedSourcesList.setEnabled( enabled );
		remainingSourcesList.setEnabled( enabled );
		removeGroup.setEnabled( !protectedGroups.contains( sourceIndexHelper.getCurrentGroup() ) );
		super.setEnabled( enabled );
	}

	/**
	 * A combobox renderer displaying the visibility state of the groups.
	 */
	class GroupComboBoxRenderer extends JLabel implements ListCellRenderer< SourceGroup >
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends SourceGroup > list, final SourceGroup value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null )
			{
				setIcon( null );
				setText( null );
				setToolTipText( null );
			}
			else if ( value.equals( NEW_GROUP ) )
			{
				setIcon( null );
				setText( "<html><b>&lt;New Group&gt;</b></html>" );
				setToolTipText( "Add a new group." );
			}
			else
			{
				setIcon( smallVisibilityIcon( value.isActive() ) );
				setText( value.getName() );
				setToolTipText( value.getName() );
			}

			setForeground( isSelected ? Color.gray : FOREGROUND_COLOR );
			return this;
		}
	}

	// A white look and feel for scroll bars.
	private final class WhiteScrollBarUI extends BasicScrollBarUI
	{
		@Override
		protected void configureScrollBarColors()
		{
			LookAndFeel.installColors( scrollbar, "ScrollBar.background", "ScrollBar.foreground" );
			thumbHighlightColor = BACKGROUND_COLOR;
			thumbLightShadowColor = BACKGROUND_COLOR;
			thumbDarkShadowColor = BACKGROUND_COLOR;
			thumbColor = Color.lightGray;
			trackColor = BACKGROUND_COLOR;
			trackHighlightColor = BACKGROUND_COLOR;
		}

		@Override
		protected JButton createDecreaseButton( final int orientation )
		{
			final BasicArrowButton button = new BasicArrowButton( orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR );
			button.setBorder( new LineBorder( BACKGROUND_COLOR ) );
			button.setBackground( BACKGROUND_COLOR );
			return button;
		}

		@Override
		protected JButton createIncreaseButton( final int orientation )
		{
			final BasicArrowButton button = new BasicArrowButton( orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR );
			button.setBorder( new LineBorder( BACKGROUND_COLOR ) );
			button.setBackground( BACKGROUND_COLOR );
			return button;
		}
	}

	class SourceEntry implements Comparable< SourceEntry >
	{

		private final SourceNameBimap sourceNames;

		private Source source;

		public SourceEntry( final Source source, final SourceNameBimap sourceNames )
		{
			this.source = source;
			this.sourceNames = sourceNames;
		}

		public String getName()
		{
			return sourceNames.getName( source );
		}

		@Override
		public int compareTo( final SourceEntry o )
		{
			// TODO: In the multi-channel case getNames() can return null.
			if ( o.getName() == null)
				return -1;
			else
				return getName().compareTo( o.getName() );
		}

		@Override
		public String toString()
		{
			return sourceNames.getName( source );
		}
	}

	class SourceListModel extends AbstractListModel< SourceEntry >
	{

		private final SourceNameBimap sourceNames;

		private TreeSet< SourceEntry > elements;

		public SourceListModel( final SourceNameBimap sourceNames )
		{
			this.elements = new TreeSet<>();
			this.sourceNames = sourceNames;
		}

		@Override
		public int getSize()
		{
			return this.elements.size();
		}

		@Override
		public SourceEntry getElementAt( final int index )
		{
			return ( SourceEntry ) this.elements.toArray()[ index ];
		}

		public void add( final Source source )
		{
			if ( elements.add( new SourceEntry( source, sourceNames ) ) )
			{
				fireContentsChanged( this, 0, getSize() );
			}
		}

		public void remove( final SourceEntry source )
		{
			if ( elements.remove( source ) )
			{
				fireContentsChanged( this, 0, getSize() );
			}
		}

		public void add( final SourceEntry sourceEntry )
		{
			if ( elements.add( sourceEntry ) )
			{
				fireContentsChanged( this, 0, getSize() );
			}
		}

		public void removeAll()
		{
			if ( elements.removeAll( elements ) )
			{
				fireContentsChanged( this, 0, getSize() );
			}
		}
	}
}
