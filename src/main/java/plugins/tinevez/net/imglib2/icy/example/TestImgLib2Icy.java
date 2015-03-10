package plugins.tinevez.net.imglib2.icy.example;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.img.Img;
import net.imglib2.img.display.icy.IcyFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestImgLib2Icy< T extends RealType< T > & NativeType< T >> extends PluginActionable
{

	@Override
	public void run()
	{
		final IcyBufferedImage image = getActiveImage();
		final Img< T > img = IcyFunctions.wrap( image );

		final PeronaMalikAnisotropicDiffusion< T > algo = new PeronaMalikAnisotropicDiffusion< T >( img, 0.15, 15 );
		algo.setNumThreads();
		if ( !algo.checkInput() )
		{
			System.out.println( "Check input failed! With: " + algo.getErrorMessage() );
			return;
		}

		final int niter = 20;
		for ( int i = 0; i < niter; i++ )
		{
			System.out.println( "Iteration " + ( i + 1 ) + " of " + niter + "." );
			algo.process();
			image.dataChanged();
		}

		System.out.println( "Done in " + algo.getProcessingTime() + " ms." );
		MessageDialog.showDialog( "TestImgLib2Icy is working fine !" );
	}

	public static < T extends RealType< T > & NativeType< T >> void main( final String[] args ) throws UnsupportedFormatException, IOException
	{
		// Launch Icy.
		Icy.main( new String[] { "--nosplash" } );

		final File file = new File( "/Users/tinevez/Desktop/iconas/Data/clown.tif" );
		final IcyBufferedImage colorImage = Loader.loadImage( file.getAbsolutePath() );
		final IcyBufferedImage image = colorImage.getImage( 0 );
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				Icy.getMainInterface().setActiveViewer( new Viewer( new Sequence( image ) ) );
				new Thread( "TestImgLib2" )
				{
					@Override
					public void run()
					{
						new TestImgLib2Icy< T >().run();
					}
				}.start();
			}
		} );

	}

}
