package plugins.tinevez.net.imglib2.icy.example;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.PluginActionable;
import icy.type.DataType;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.real.FloatType;

public class TestImgLib2Icy extends PluginActionable
{

	@Override
	public void run()
	{

		final IcyBufferedImage image = getActiveImage();

		final long[] dims = new long[] { image.getWidth(), image.getHeight() };

		final DataType dataType = image.getDataType_();

		final float[] arr = image.getDataXYAsFloat( 0 );

//			img = ArrayImgs.floats( arr, dims );
		final PlanarImg< FloatType, FloatArray > img = PlanarImgs.floats( dims );
		img.setPlane( 0, new FloatArray( arr ) );

		final PeronaMalikAnisotropicDiffusion algo = new PeronaMalikAnisotropicDiffusion( img, 0.15, 0.5 );

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

}
