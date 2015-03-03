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
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;

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
		
		// Wrap the wrapped image in an Icy sequence, and display it in Icy.
		final VirtualSequence sequence = new VirtualSequence( img );
		SwingUtilities.invokeAndWait( new Runnable()
		{
			@Override
			public void run()
			{
				System.out.print( "Displaying in Icy... " );
				final Viewer viewer = new Viewer( sequence );
				Icy.getMainInterface().setActiveViewer( viewer );
				System.out.println( "Done.\n" );
			}
		} );
	}
}
