package plugins.tinevez.net.imglib2.icy.example;

import icy.gui.viewer.Viewer;
import icy.main.Icy;
import ij.ImageJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import net.imglib2.img.Img;
import net.imglib2.img.display.icy.VirtualSequence;
import net.imglib2.img.display.icy.VirtualSequence.DimensionArrangement;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class VirtualSequenceDemo
{

	public static void main( final String[] args ) throws ImgIOException, InterruptedException, InvocationTargetException
	{
		// Launch ImageJ.
		ImageJ.main( args );

		// Launch Icy.
		Icy.main( args );

		final File file = new File( "/Users/tinevez/Desktop/iconas/Data/Celegans-5pc.tif" );
		
		// Load an image and display it in ImageJ.
		final ImagePlus imp = new ImagePlus( file.getAbsolutePath() );
		imp.setOpenAsHyperStack( true );
		imp.show();

		// Wrap it in an ImgLib2 container.
		final Img img = ImageJFunctions.wrap( imp );
		System.out.println( "Loaded " + img + " - " + Util.printInterval( img ) );
		
		// Reslice through its Z center.
		final IntervalView slice = Views.hyperSlice( img, 2, 21 );
		final DimensionArrangement arrangement = DimensionArrangement.XYT;

		// Reslice at a given time.
		final IntervalView slice2 = Views.hyperSlice( img, 3, 49 );
		final DimensionArrangement arrangement2 = DimensionArrangement.XYZ;

		// Wrap the wrapped images in Icy sequences, and display them in Icy.
		final VirtualSequence sequence = new VirtualSequence( slice, arrangement );
		final VirtualSequence sequence2 = new VirtualSequence( slice2, arrangement2 );

		SwingUtilities.invokeAndWait( new Runnable()
		{
			@Override
			public void run()
			{
				System.out.print( "Displaying first sequence in Icy... " );
				final Viewer viewer = new Viewer( sequence );
				System.out.println( "Done.\n" );

				System.out.print( "Displaying Second sequence in Icy... " );
				final Viewer viewer2 = new Viewer( sequence2 );
				System.out.println( "Done.\n" );
			}
		} );
	}
}
