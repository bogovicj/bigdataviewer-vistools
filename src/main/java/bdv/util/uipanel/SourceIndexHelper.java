package bdv.util.uipanel;

import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import java.util.List;

/**
 * Helpers for using sources instead of indices...
 */
class SourceIndexHelper
{
	private final VisibilityAndGrouping visibility;

	private final ViewerPanel viewer;

	public SourceIndexHelper( final VisibilityAndGrouping visibility, final ViewerPanel viewer )
	{
		this.visibility = visibility;
		this.viewer = viewer;
	}

	public Source< ? > getCurrentSource()
	{
		final ViewerState state = viewer.getState();
		final int i = state.getCurrentSource();
		return getSourceForIndex( state, i );
	}

	public boolean isSourceActive( final Source< ? > source )
	{
		return getSourceState( viewer.getState(), source ).isActive();
	}

	public int getSourceIndex( final Source< ? > source )
	{
		return getIndex( viewer.getState(), source );
	}

	public SourceGroup getCurrentGroup()
	{
		final ViewerState state = viewer.getState();
		return getGroupForIndex( state, state.getCurrentGroup() );
	}

	public void setCurrentGroup( final SourceGroup group )
	{
		final int i = visibility.getSourceGroups().indexOf( group );
		if ( i >= 0 )
			visibility.setCurrentGroup( i );
	}

	public boolean contains( final SourceGroup group, final Source< ? > source )
	{
		final ViewerState state = viewer.getState();
		final int sourceIndex = getIndex( state, source );
		return group.getSourceIds().contains( sourceIndex );
	}

	@Deprecated
	public void addSourceToGroup( final Source< ? > source, final int groupIndex /*TODO*/ )
	{
		final ViewerState state = viewer.getState();
		final int sourceIndex = getIndex( state, source );
		visibility.addSourceToGroup( sourceIndex, groupIndex );
	}

	public void addSourceToGroup( final Source< ? > source, final SourceGroup group )
	{
		final ViewerState state = viewer.getState();
		final int sourceIndex = getIndex( state, source );
		final int groupIndex = getIndex( state, group );
		visibility.addSourceToGroup( sourceIndex, groupIndex );
	}

	@Deprecated
	public void removeSourceFromGroup( final Source< ? > source, final int groupIndex /* TODO */ )
	{
		final ViewerState state = viewer.getState();
		final int sourceIndex = getIndex( state, source );
		visibility.removeSourceFromGroup( sourceIndex, groupIndex );
	}

	public void removeSourceFromGroup( final Source< ? > source, final SourceGroup group )
	{
		final ViewerState state = viewer.getState();
		final int sourceIndex = getIndex( state, source );
		final int groupIndex = getIndex( state, group );
		visibility.removeSourceFromGroup( sourceIndex, groupIndex );
	}

	public void setGroupActive( final SourceGroup group, final boolean active )
	{
		final int i = visibility.getSourceGroups().indexOf( group );
		if ( i >= 0 )
			visibility.setGroupActive( i, active );
	}

	// = = = = = = = = = = = = = = = = = =

	private SourceState< ? > getSourceState( final ViewerState state, final Source< ? > source )
	{
		final List< SourceState< ? > > ss = state.getSources();
		for ( final SourceState< ? > s : ss )
			if ( s.getSpimSource() == source )
				return s;
		return null;
	}

	private int getIndex( final ViewerState state, final Source< ? > source )
	{
		final List< SourceState< ? > > ss = state.getSources();
		for ( int i = 0; i < ss.size(); ++i )
		{
			final SourceState< ? > s = ss.get( i );
			if ( s.getSpimSource() == source )
				return i;
		}
		return -1;
	}

	private Source< ? > getSourceForIndex( final ViewerState state, final int sourceIndex )
	{
		final List< SourceState< ? > > ss = state.getSources();
		if ( sourceIndex < 0 || sourceIndex > ss.size() )
			return null;
		return ss.get( sourceIndex ).getSpimSource();
	}

	private int getIndex( final ViewerState state, final SourceGroup group )
	{
		return state.getSourceGroups().indexOf( group );
	}

	private SourceGroup getGroupForIndex( final ViewerState state, final int groupIndex )
	{
		final List< SourceGroup > groups = state.getSourceGroups();
		if ( groupIndex < 0 || groupIndex > groups.size() )
			return null;
		return groups.get( groupIndex );
	}
}
