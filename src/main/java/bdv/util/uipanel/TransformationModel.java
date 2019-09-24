package bdv.util.uipanel;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.ManualTransformActiveListener;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.util.PlaceHolderSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

public class TransformationModel implements ManualTransformActiveListener, BdvHandle.SourceChangeListener
{

	private final ViewerPanel viewerPanel;

	private final VisibilityAndGrouping visGro;

	private final TriggerBehaviourBindings triggerBindings;

	private final ManualTransformationEditor manualTransformationEditor;

	private final SourceIndexHelper sourceIndexHelper;

	private final Map< Source< ? >, AffineTransform3D > transformationLookup = new HashMap<>();

	public TransformationModel( final ViewerPanel viewerPanel, final TriggerBehaviourBindings triggerBindings,
			final ManualTransformationEditor manualTransformationEditor )
	{
		this.viewerPanel = viewerPanel;
		this.triggerBindings = triggerBindings;
		this.manualTransformationEditor = manualTransformationEditor;

		visGro = this.viewerPanel.getVisibilityAndGrouping();
		this.sourceIndexHelper = new SourceIndexHelper( visGro, viewerPanel );
	}

	public void enableManualTransformation( final boolean selected )
	{
		this.manualTransformActiveChanged( selected );
	}

	@Override
	public void manualTransformActiveChanged( final boolean active )
	{
		manualTransformationEditor.setActive( active );
		if ( !active )
			saveTransformation();
	}

	public void addManualTransformActiveListener( final ManualTransformActiveListener listener )
	{
		this.manualTransformationEditor.manualTransformActiveListeners().add( listener );
	}

	public void reset( final boolean singleSourceTransformation )
	{
		if ( !( sourceIndexHelper.getCurrentSource() instanceof PlaceHolderSource ) )
		{
			if ( singleSourceTransformation )
			{
				manualTransformationEditor.reset();
			}
			else
			{
				for ( SourceState< ? > sourceState : viewerPanel.getState().getSources() )
				{
					final Source< ? > source = sourceState.getSpimSource();
					if ( source instanceof TransformedSource )
					{
						( ( TransformedSource< ? > ) source ).setFixedTransform( new AffineTransform3D() );
						( ( TransformedSource< ? > ) source ).setIncrementalTransform( new AffineTransform3D() );
						( ( TransformedSource< ? > ) source )
								.setIncrementalTransform( transformationLookup.get( source ) );
					}
				}
				viewerPanel.setCurrentViewerTransform( createViewerInitTransformation() );
			}
		}
	}

	/**
	 * Save transformation on selected source/group.
	 */
	public void saveTransformation()
	{
		System.out.println( "save transformation" );
		final AffineTransform3D t = new AffineTransform3D();
		if ( visGro.getDisplayMode() == DisplayMode.GROUP || visGro.getDisplayMode() == DisplayMode.FUSEDGROUP )
		{
			final SourceGroup currentGroup = sourceIndexHelper.getCurrentGroup();
			final List< SourceState< ? > > sources = viewerPanel.getState().getSources();
			for ( int id : currentGroup.getSourceIds() )
			{
				final Source< ? > s = sources.get( id ).getSpimSource();
				saveCurrentSourceTransform( t, s );
			}
		}
		else
		{
			final Source< ? > currentSource = sourceIndexHelper.getCurrentSource();
			if ( currentSource != null )
			{
				saveCurrentSourceTransform( t, currentSource );
				if ( currentSource instanceof TransformedSource )
				{
					( ( TransformedSource< ? > ) currentSource ).getFixedTransform( t );
				}
				transformationLookup
						.put( currentSource, t );
			}

		}
	}

	private void saveCurrentSourceTransform( final AffineTransform3D t, final Source< ? > s )
	{
		if ( s instanceof TransformedSource )
		{
			System.out.println( s.getName() );
			( ( TransformedSource< ? > ) s ).getFixedTransform( t );
		}
		else if ( s instanceof PlaceHolderSource )
		{
			// TODO: But what?
		}
		transformationLookup.put( s, t );
	}

	public void enableRotation( final boolean selected )
	{
		if ( selected )
			triggerBindings.removeBehaviourMap( "blockRotation" );
		else
			blockRotation();
	}

	/**
	 * Add empty behaviours to block rotation.
	 */
	private void blockRotation()
	{
		final BehaviourMap blockRotation = new BehaviourMap();
		blockRotation.put( "rotate left", new Behaviour()
		{
		} );
		blockRotation.put( "rotate left slow", new Behaviour()
		{
		} );
		blockRotation.put( "rotate left fast", new Behaviour()
		{
		} );

		blockRotation.put( "rotate right", new Behaviour()
		{
		} );
		blockRotation.put( "rotate right slow", new Behaviour()
		{
		} );
		blockRotation.put( "rotate right fast", new Behaviour()
		{
		} );

		blockRotation.put( "drag rotate", new Behaviour()
		{
		} );
		blockRotation.put( "drag rotate slow", new Behaviour()
		{
		} );
		blockRotation.put( "drag rotate fast", new Behaviour()
		{
		} );

		// 2D
		blockRotation.put( "2d drag rotate", new Behaviour()
		{
		} );
		blockRotation.put( "2d scroll rotate", new Behaviour()
		{
		} );
		blockRotation.put( "2d scroll rotate slow", new Behaviour()
		{
		} );
		blockRotation.put( "2d scroll rotate fast", new Behaviour()
		{
		} );
		blockRotation.put( "2d scroll translate", new Behaviour()
		{
		} );
		blockRotation.put( "2d rotate left", new Behaviour()
		{
		} );
		blockRotation.put( "2d rotate right", new Behaviour()
		{
		} );
		triggerBindings.addBehaviourMap( "blockRotation", blockRotation );
	}

	public void enableTranslation( final boolean selected )
	{
		if ( selected )
			triggerBindings.removeBehaviourMap( "blockTranslation" );
		else
			blockTranslation();
	}

	/**
	 * Add empty behaviours to block translation.
	 */
	private void blockTranslation()
	{
		final BehaviourMap blockTranslation = new BehaviourMap();
		blockTranslation.put( "drag translate", new Behaviour()
		{
		} );

		// 2D
		blockTranslation.put( "2d drag translate", new Behaviour()
		{
		} );

		triggerBindings.addBehaviourMap( "blockTranslation", blockTranslation );
	}

	/**
	 * Compute initial transformation.
	 *
	 * @return the transformation.
	 */
	private AffineTransform3D createViewerInitTransformation()
	{
		final double cX = viewerPanel.getWidth() / 2d;
		final double cY = viewerPanel.getHeight() / 2d;
		ViewerState state = viewerPanel.getState();
		if ( state.getCurrentSource() < 0 )
		{
			return new AffineTransform3D();
		}
		final Source< ? > source = state.getSources().get( state.getCurrentSource() ).getSpimSource();
		final int timepoint = state.getCurrentTimepoint();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = ( int ) ( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		if ( ( sX1 - sX0 ) >= ( sY1 - sY0 ) )
		{
			viewerTransform.scale( viewerPanel.getWidth() / ( 4.0 * ( sX1 - sX0 ) ) );
		}
		else
		{
			viewerTransform.scale( viewerPanel.getHeight() / ( 4.0 * ( sY1 - sY0 ) ) );
		}

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );

		return viewerTransform;
	}

	@Override
	public void sourceAdded( final Source< ? > source, final ConverterSetup converterSetup )
	{
		transformationLookup.put( source,
				getInitialTransformation( source ) );
	}

	/**
	 * Extract the transformation of the source with sourceIdx.
	 *
	 * @param source
	 *
	 * @return transformation
	 */
	private AffineTransform3D getInitialTransformation( final Source< ? > source )
	{
		final AffineTransform3D t = new AffineTransform3D();
		if ( source instanceof TransformedSource )
		{
			( ( TransformedSource< ? > ) source ).getFixedTransform( t );
		}
		return t;
	}

	@Override
	public void sourceRemoved( final Source< ? > source )
	{
		transformationLookup.remove( source );
	}
}
