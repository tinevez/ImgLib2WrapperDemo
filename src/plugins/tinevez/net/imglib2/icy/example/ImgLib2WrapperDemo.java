package plugins.tinevez.net.imglib2.icy.example;

import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

import java.awt.Color;
import java.util.List;

import net.imglib2.Point;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.IcySequenceAdapter;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import plugins.kernel.roi.roi2d.ROI2DEllipse;

public class ImgLib2WrapperDemo extends PluginActionable
{

	@Override
	public void run()
	{
		final Sequence sequence = getActiveSequence();

		final PlanarImg< ?, ? > img = IcySequenceAdapter.wrap( sequence );
		final double[] calibration = IcySequenceAdapter.getCalibration( sequence );

		System.out.println( "Received a " + Util.printInterval( img ) + " sequence, with calibration = "
				+ Util.printCoordinates( calibration ) );
		

		final double sigma1 = 40.;
		final double sigma2 = 60.;
		final double minPeakValue = 0.0;
		final DogDetection< ? > dog = new DogDetection( Views.extendMirrorSingle( img ), img, calibration, sigma1, sigma2, ExtremaType.MAXIMA, minPeakValue, false );
		dog.setNumThreads( 1 );
		final List< RefinedPeak< Point >> peaks = dog.getSubpixelPeaks();

		System.out.println( "Found " + peaks.size() + " bright peaks." );

		for ( final RefinedPeak< Point > peak : peaks )
		{
			System.out.println( peak.getOriginalPeak() + " -> " + peak.getValue() );// DEBUG

			final double xmin = peak.getDoublePosition( 0 ) - sigma2;
			final double ymin = peak.getDoublePosition( 1 ) - sigma2;
			final double xmax = peak.getDoublePosition( 0 ) + sigma2;
			final double ymax = peak.getDoublePosition( 1 ) + sigma2;
			final ROI2DEllipse roi = new ROI2DEllipse( xmin, ymin, xmax, ymax );
			sequence.addROI( roi );
		}

		final DogDetection< ? > dog2 = new DogDetection( Views.extendMirrorSingle( img ), img, calibration, sigma1, sigma2, ExtremaType.MINIMA, minPeakValue, false );
		dog.setNumThreads( 1 );
		final List< RefinedPeak< Point >> peaks2 = dog2.getSubpixelPeaks();

		System.out.println( "Found " + peaks2.size() + " dark peaks." );

		for ( final RefinedPeak< Point > peak : peaks2 )
		{
			System.out.println( peak.getOriginalPeak() + " -> " + peak.getValue() );// DEBUG

			final double xmin = peak.getDoublePosition( 0 ) - sigma2;
			final double ymin = peak.getDoublePosition( 1 ) - sigma2;
			final double xmax = peak.getDoublePosition( 0 ) + sigma2;
			final double ymax = peak.getDoublePosition( 1 ) + sigma2;
			final ROI2DEllipse roi = new ROI2DEllipse( xmin, ymin, xmax, ymax );
			roi.setColor( Color.RED );
			sequence.addROI( roi );
		}



	}

}
