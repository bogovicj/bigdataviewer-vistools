package bdv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.fitting.ellipsoid.Ellipsoid;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

/**
 * all show methods return a {@link Bdv} which can be used to add more stuff to the same window
 *
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BdvFunctions
{
	public static < T > BdvStackSource< T > show(
			final RandomAccessibleInterval< T > img,
			final String name,
			final BdvOptions options )
	{
		return show( null, img, name, options );
	}

	public static < T > BdvStackSource< T > show(
			final RandomAccessibleInterval< T > img,
			final String name )
	{
		return show( null, img, name );
	}

	public static < T > BdvStackSource< T > show(
			final Bdv bdv,
			final RandomAccessibleInterval< T > img,
			final String name )
	{
		return show( bdv, img, name, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final Bdv bdv,
			final RandomAccessibleInterval< T > img,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		final T type = Util.getTypeFromInterval( img );
		if ( type instanceof ARGBType )
		{
			@SuppressWarnings( "unchecked" )
			final BdvStackSource< T > stackSource = ( BdvStackSource< T > ) addStackSourceARGBType(
					handle,
					( RandomAccessibleInterval< ARGBType > ) img,
					name,
					sourceTransform );
			return stackSource;
		}
		else if ( type instanceof RealType )
		{
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			final BdvStackSource< ? > tmp = addStackSourceRealType(
					handle,
					( RandomAccessibleInterval< RealType > ) img,
					name,
					sourceTransform );
			@SuppressWarnings( "unchecked" )
			final BdvStackSource< T > stackSource = ( BdvStackSource< T > ) tmp;
			return stackSource;
		}

		return null;
	}

	private static BdvStackSource< ARGBType > addStackSourceARGBType(
			final BdvHandle handle,
			final RandomAccessibleInterval< ARGBType > img,
			final String name,
			final AffineTransform3D sourceTransform )
	{
		final Source< ARGBType > s = new RandomAccessibleIntervalSource<>( img, new ARGBType(), sourceTransform, name );
		final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s );
		final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );
		final SourceAndConverter< ARGBType > soc = new SourceAndConverter< ARGBType >( ts, converter );

		final int setupId = handle.getUnusedSetupId();
		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Arrays.asList( setup ) );
		final List< SourceAndConverter< ARGBType > > sources = new ArrayList<>( Arrays.asList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< ARGBType > bdvSource = new BdvStackSource<>( handle, numTimepoints, new ARGBType(), converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	private static < T extends RealType< T > > BdvStackSource< T > addStackSourceRealType(
			final BdvHandle handle,
			final RandomAccessibleInterval< T > img,
			final String name,
			final AffineTransform3D sourceTransform )
	{
		final T type = Util.getTypeFromInterval( img );
		final Source< T > s = new RandomAccessibleIntervalSource<>( img, type, sourceTransform, name );
		final TransformedSource< T > ts = new TransformedSource< T >( s );
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1< T >( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );
		final SourceAndConverter< T > soc = new SourceAndConverter< T >( ts, converter );

		final int setupId = handle.getUnusedSetupId();
		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Arrays.asList( setup ) );
		final List< SourceAndConverter< T > > sources = new ArrayList<>( Arrays.asList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static BdvPointsSource showPoints(
			final List< ? extends RealLocalizable > points,
			final String name,
			final BdvOptions options )
	{
		return showPoints( null, points, name, options );
	}

	public static BdvPointsSource showPoints(
			final List< ? extends RealLocalizable > points,
			final String name )
	{
		return showPoints( null, points, name );
	}

	public static BdvPointsSource showPoints(
			final Bdv bdv,
			final List< ? extends RealLocalizable > points,
			final String name )
	{
		return showPoints( bdv, points, name, Bdv.options() );
	}

	public static BdvPointsSource showPoints(
			final Bdv bdv,
			final List< ? extends RealLocalizable > points,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();

		final int setupId = handle.getUnusedSetupId();
		final ARGBType defaultColor = new ARGBType( 0xff00ff00 );
		final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, defaultColor );
		final PlaceHolderSource source = new PlaceHolderSource( name );
		final SourceAndConverter< UnsignedShortType > soc = new SourceAndConverter<>( source, null );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Arrays.asList( setup ) );
		final List< SourceAndConverter< UnsignedShortType > > sources = new ArrayList<>( Arrays.asList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );

		final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), source, setup );
		final PointsOverlay overlay = new PointsOverlay();
		overlay.setOverlayInfo( info );
		overlay.setPoints( points );
		overlay.setSourceTransform( sourceTransform );
		handle.getViewerPanel().getDisplay().addOverlayRenderer( overlay );

		final BdvPointsSource bdvSource = new BdvPointsSource( handle, numTimepoints, setup, soc, info, overlay );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static BdvEllipsoidSource showEllipsoid(
			final Ellipsoid ellipsoid,
			final String name,
			final BdvOptions options )
	{
		return showEllipsoid( null, ellipsoid, name, options );
	}

	public static BdvEllipsoidSource showEllipsoid(
			final Ellipsoid ellipsoid,
			final String name )
	{
		return showEllipsoid( null, ellipsoid, name );
	}

	public static BdvEllipsoidSource showEllipsoid(
			final Bdv bdv,
			final Ellipsoid ellipsoid,
			final String name )
	{
		return showEllipsoid( bdv, ellipsoid, name, Bdv.options() );
	}

	public static BdvEllipsoidSource showEllipsoid(
			final Bdv bdv,
			final Ellipsoid ellipsoid,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();

		final int setupId = handle.getUnusedSetupId();
		final ARGBType defaultColor = new ARGBType( 0xff00ff00 );
		final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, defaultColor );
		final PlaceHolderSource source = new PlaceHolderSource( name );
		final SourceAndConverter< UnsignedShortType > soc = new SourceAndConverter<>( source, null );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Arrays.asList( setup ) );
		final List< SourceAndConverter< UnsignedShortType > > sources = new ArrayList<>( Arrays.asList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );

		final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), source, setup );
		final EllipsoidOverlay overlay = new EllipsoidOverlay();
		overlay.setOverlayInfo( info );
		overlay.setEllipsoid( ellipsoid );
		overlay.setSourceTransform( sourceTransform );
		handle.getViewerPanel().getDisplay().addOverlayRenderer( overlay );

		final BdvEllipsoidSource bdvSource = new BdvEllipsoidSource( handle, numTimepoints, setup, soc, info, overlay );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static BdvEllipsoidsSource showEllipsoids(
			final Bdv bdv,
			final List< Ellipsoid > ellipsoids,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();

		final int setupId = handle.getUnusedSetupId();
		final ARGBType defaultColor = new ARGBType( 0xff00ff00 );
		final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, defaultColor );
		final PlaceHolderSource source = new PlaceHolderSource( name );
		final SourceAndConverter< UnsignedShortType > soc = new SourceAndConverter<>( source, null );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Arrays.asList( setup ) );
		final List< SourceAndConverter< UnsignedShortType > > sources = new ArrayList<>( Arrays.asList( soc ) );

		final int numTimepoints = ellipsoids.size();
		handle.add( converterSetups, sources, numTimepoints );

		final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), source, setup );
		final EllipsoidsOverlay overlay = new EllipsoidsOverlay();
		overlay.setOverlayInfo( info );
		overlay.setEllipsoids( ellipsoids );
		overlay.setSourceTransform( sourceTransform );
		handle.getViewerPanel().getDisplay().addOverlayRenderer( overlay );

		final BdvEllipsoidsSource bdvSource = new BdvEllipsoidsSource( handle, numTimepoints, setup, soc, info, overlay );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	// TODO: move to BdvFunctionUtils
	public static int getUnusedSetupId( final BigDataViewer bdv )
	{
		return getUnusedSetupId( bdv.getSetupAssignments() );
	}

	// TODO: move to BdvFunctionUtild
	public static int getUnusedSetupId( final SetupAssignments setupAssignments )
	{
		int maxId = 0;
		for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
			maxId = Math.max( setup.getSetupId(), maxId );
		return maxId + 1;
	}
}
