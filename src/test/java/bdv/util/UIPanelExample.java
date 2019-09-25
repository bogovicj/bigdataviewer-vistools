package bdv.util;

import bdv.util.uipanel.BdvUIPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

public class UIPanelExample
{
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final ArrayImg< ARGBType, IntArray > img = ArrayImgs.argbs( 100, 100, 2 );
		final ArrayImg< FloatType, FloatArray > img_float = ArrayImgs.floats( 200, 100 );
		final Random random = new Random();
		img.forEach( t -> t.set( random.nextInt() ) );
		img_float.forEach( t -> t.set( random.nextFloat() * 65000 ) );

		final JFrame frame = new JFrame( "my test frame" );
		final BdvUIPanel bdv = new BdvUIPanel( frame, Bdv.options().is2D() );
		frame.add( bdv.getSplitPane(), BorderLayout.CENTER );
		bdv.getSplitPane().setDividerLocation( 0.8 );
		frame.setPreferredSize( new Dimension( 800, 600 ) );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );

		final BdvOverlay overlay = createBdvOverlay( random );
//		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ).axisOrder( AxisOrder.XY ) );

//		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ).axisOrder( AxisOrder.XYC ) );
		for (int i = 0; i < 140; i++)
			BdvFunctions.show( img_float, "test", Bdv.options().addTo( bdv ).axisOrder( AxisOrder.XY ) );

		// Action to open and close the BDV-UI panel via P-key.
		// TODO: Remove 
		Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( bdv.getBdvHandle().getKeybindings(), "my-new-actions" );

		actions.runnableAction( () -> {
			((BdvUIPanel)bdv).collapseUI();
		}, "print global pos", "P" );
	}

	private static BdvOverlay createBdvOverlay( final Random random )
	{
		final ArrayList< RealPoint > points = new ArrayList<>();
		for ( int i = 0; i < 1100; ++i )
			points.add( new RealPoint( random.nextInt( 100 ), random.nextInt( 100 ) ) );

		return new BdvOverlay()
		{
			@Override
			protected void draw( final Graphics2D g )
			{
				final AffineTransform2D t = new AffineTransform2D();
				getCurrentTransform2D( t );

				g.setColor( Color.RED );

				final double[] lPos = new double[ 2 ];
				final double[] gPos1 = new double[ 2 ];
				final double[] gPos2 = new double[ 2 ];
				final int start = info.getTimePointIndex() * 10;
				final int end = info.getTimePointIndex() * 10 + 100;
				for ( int i = start; i < end; i+=2 )
				{
					points.get( i ).localize( lPos );
					t.apply( lPos, gPos1 );
					points.get( i + 1 ).localize( lPos );
					t.apply( lPos, gPos2 );
					g.drawLine( ( int ) gPos1[ 0 ], ( int ) gPos1[ 1 ], ( int ) gPos2[ 0 ], ( int ) gPos2[ 1 ] );
				}
			}
		};
	}
}
