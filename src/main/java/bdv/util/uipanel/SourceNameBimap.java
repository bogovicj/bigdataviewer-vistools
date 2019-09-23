package bdv.util.uipanel;

import bdv.viewer.Source;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps sources to/from unique names.
 */
class SourceNameBimap
{
	private final Map< Source< ? >, String > sourceToName = new HashMap<>();
	private final Map< String, Source< ? > > nameToSource = new HashMap<>();

	public synchronized String add( final Source< ? > source )
	{
		if ( contains( source ) )
			throw new IllegalArgumentException();

		final String name = uniqueName( source.getName() );
		sourceToName.put( source, name );
		nameToSource.put( name, source );
		return name;
	}

	public synchronized String remove( final Source< ? > source )
	{
		final String name = sourceToName.remove( source );
		if ( name != null )
			nameToSource.remove( name );
		return name;
	}

	public synchronized boolean contains( final Source< ? > source )
	{
		return sourceToName.containsKey( source );
	}

	public synchronized String getName( final Source< ? > source )
	{
		return sourceToName.get( source );
	}

//		public String getName( final SourceState< ? > source )
//		{
//			return getName( source.getSpimSource() );
//		}

	public synchronized Source< ? > getSource( final String name )
	{
		return nameToSource.get( name );
	}

	private String uniqueName( final String name )
	{
		String prefix = "";
		int i = 0;
		while ( nameToSource.containsKey( prefix + name ) )
		{
			prefix = i + "_";
			i++;
		}
		return prefix + name;
	}
}
