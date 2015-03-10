package plugins.tinevez.net.imglib2.icy.example;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory;
import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.viewer.Viewer;
import icy.image.colormap.IceColorMap;
import icy.image.colormap.IcyColorMap;
import icy.image.colormap.JETColorMap;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.IcySequenceAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPath;

public class ImgLib2WrapperDemo extends PluginActionable
{

	@Override
	public void run()
	{
		ThreadUtil.bgRun( new Runnable()
		{
			@SuppressWarnings( "unchecked" )
			@Override
			public void run()
			{
				final Sequence sequence = getActiveSequence();

				final Img img = rawWrap( sequence );
				final double[] calibration = IcySequenceAdapter.getCalibration( sequence );

				System.out.println( "Received a " + Util.printInterval( img ) + " sequence, with calibration = "
						+ Util.printCoordinates( calibration ) );

				final double radius = 2.5;
				final double minPeakValue = 5.0;

				// We iterate along the last dimension.
				final int d;
				final long nFrames;
				int nDims;
				if ( img.numDimensions() > 2 )
				{
					d = img.numDimensions() - 1;
					nFrames = img.dimension( d );
					nDims = img.numDimensions() - 1;
				}
				else
				{
					d = -1;
					nFrames = 1;
					nDims = img.numDimensions();
				}
				final double sigma1 = radius / Math.sqrt( nDims ) * 1.1;
				final double sigma2 = radius / Math.sqrt( nDims ) * 0.9;

				final List< List< RefinedPeak< Point >>> results = new ArrayList< List< RefinedPeak< Point >> >( ( int ) nFrames );

				for ( int t = 0; t < nFrames; t++ )
				{
					@SuppressWarnings( "rawtypes" )
					final IterableInterval slice;
					if ( d > 0 )
					{
						slice = Views.hyperSlice( img, d, t );
					}
					else
					{
						slice = img;
					}
					final Img< FloatType > target = ImgLib2Utils.copyToFloat( slice );

					final DogDetection< FloatType > dog = new DogDetection< FloatType >( Views.extendMirrorSingle( target ), target, calibration, sigma1, sigma2, ExtremaType.MAXIMA, minPeakValue, false );
					dog.setNumThreads( 1 );

					final List< RefinedPeak< Point >> peaks = dog.getSubpixelPeaks();
					results.add( t, peaks );
				}

				/*
				 * Find max & min.
				 */
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;
				for ( final List< RefinedPeak< Point >> peaks : results )
				{
					for ( final RefinedPeak< Point > peak : peaks )
					{
						final double val = peak.getValue();
						if ( val > max )
						{
							max = val;
						}
						if ( val < min )
						{
							min = val;
						}
					}
				}
				/*
				 * Create ROIs and SpotCollection.
				 */

				final IcyColorMap colormap = new IceColorMap();
				final SpotCollection sc = new SpotCollection();

				for ( int t = 0; t < results.size(); t++ )
				{
					final List< RefinedPeak< Point >> peaks = results.get( t );
					final Collection< Spot > spots = new ArrayList< Spot >( peaks.size() );
					for ( final RefinedPeak< Point > peak : peaks )
					{
						// Icy ROIs.

						final double xmin = 0.5 + peak.getDoublePosition( 0 ) - radius;
						final double ymin = 0.5 + peak.getDoublePosition( 1 ) - radius;
						final double xmax = 0.5 + peak.getDoublePosition( 0 ) + radius;
						final double ymax = 0.5 + peak.getDoublePosition( 1 ) + radius;
						final int colorIndex = ( int ) ( IcyColorMap.MAX_INDEX * ( peak.getValue() - min ) / ( max - min ) );

						final ROI2DEllipse roi = new ROI2DEllipse( xmin, ymin, xmax, ymax );
						roi.setT( t );
						roi.setColor( colormap.getColor( colorIndex ) );
						roi.setReadOnly( true );

						sequence.addROI( roi );

						// Spot
						final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
						final double y = peak.getDoublePosition( 1 ) * calibration[ 1 ];
						final double z;
						if ( peak.numDimensions() > 2 )
						{
							z = peak.getDoublePosition( 2 ) * calibration[ 2 ];
						}
						else
						{
							z = 0;
						}
						final Spot spot = new Spot( x, y, z, radius, peak.getValue() );
						spots.add( spot );
					}
					sc.put( t, spots );
				}
				sc.setVisible( true );

				System.out.println( "Detection done." );

				/*
				 * Tracking.
				 */


				System.out.println( "Executing tracking." );
				final Model model = new Model();
				model.setLogger( Logger.DEFAULT_LOGGER );
				model.setSpots( sc, false );

				final SpotTrackerFactory trackerFactory = new SparseLAPTrackerFactory();
				final Map< String, Object > trackerSettings = trackerFactory.getDefaultSettings();
				trackerSettings.put( TrackerKeys.KEY_ALLOW_GAP_CLOSING, true );
				trackerSettings.put( TrackerKeys.KEY_ALLOW_TRACK_MERGING, true );
				trackerSettings.put( TrackerKeys.KEY_ALLOW_TRACK_SPLITTING, true );

				final Settings settings = new Settings();
				settings.trackerFactory = trackerFactory;
				settings.trackerSettings = trackerSettings;

				final TrackMate trackmate = new TrackMate( model, settings );
				final boolean trackingOK = trackmate.execTracking();
				if ( !trackingOK )
				{
					System.err.println( trackmate.getErrorMessage() );
					return;
				}
				System.out.println( "Found " + model.getTrackModel().nTracks( true ) + " tracks." );

				final JETColorMap trackColormap = new JETColorMap();
				int index = 0;
				for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
				{
					final Path2D trackPath = new Path2D.Double();
					for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
					{
						final Spot source = model.getTrackModel().getEdgeSource( edge );
						final double xs = 0.5 + source.getDoublePosition( 0 ) / calibration[ 0 ];
						final double ys = 0.5 + source.getDoublePosition( 1 ) / calibration[ 1 ];

						final Spot target = model.getTrackModel().getEdgeTarget( edge );
						final double xt = 0.5 + target.getDoublePosition( 0 ) / calibration[ 0 ];
						final double yt = 0.5 + target.getDoublePosition( 1 ) / calibration[ 1 ];

						trackPath.moveTo( xs, ys );
						trackPath.lineTo( xt, yt );
					}
					final ROI2DPath trackRoi = new ROI2DPath( trackPath );
					trackRoi.setColor( trackColormap.getColor( ( int ) ( ( double ) index++ * IcyColorMap.MAX_INDEX / model.getTrackModel().nTracks( true ) ) ) );
					trackRoi.setReadOnly( true );
					sequence.addROI( trackRoi );
				}

				System.out.println( "Tracking done." );
			}

		} );
	}

	public static final Img rawWrap( final Sequence sequence )
	{
		final Img< DoubleType > img = IcySequenceAdapter.wrap( sequence );
		final Img raw = img;
		return raw;
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args ) throws UnsupportedFormatException, IOException, InterruptedException, InvocationTargetException
	{
		Icy.main( new String[] { "--nosplash" } );

		final File file = new File( "../TrackMate/samples/FakeTracks.tif" );

		System.out.print( "Loading image... " );
		final Sequence sequence = Loader.loadSequence( file.getAbsolutePath(), 0, true );
		System.out.println( "Done." );

		SwingUtilities.invokeAndWait( new Runnable()
		{

			@Override
			public void run()
			{
				System.out.print( "Displaying in Icy... " );
				final Viewer viewer = new Viewer( sequence );
				Icy.getMainInterface().setActiveViewer( viewer );
				System.out.println( "Done.\n" );

				System.out.print( "Running the plugin..." );
				final ImgLib2WrapperDemo plugin = new ImgLib2WrapperDemo();
				plugin.run();
				System.out.println( "Done." );
			}
		} );
	}
}
