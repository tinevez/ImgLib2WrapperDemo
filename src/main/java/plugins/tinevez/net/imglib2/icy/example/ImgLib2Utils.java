package plugins.tinevez.net.imglib2.icy.example;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

public class ImgLib2Utils
{
	public static final < T extends RealType< T >> Img< FloatType > copyToFloat( final IterableInterval< T > source )
	{
		final ImgFactory< FloatType > factory = Util.getArrayOrCellImgFactory( source, new FloatType() );
		final Img< FloatType > target = factory.create( source, new FloatType() );

		if ( source.iterationOrder().equals( target.iterationOrder() ) )
		{
			final Cursor< T > sourceCursor = source.cursor();
			final Cursor< FloatType > targetCursor = target.cursor();
			while ( sourceCursor.hasNext() )
			{
				sourceCursor.fwd();
				targetCursor.fwd();
				targetCursor.get().set( sourceCursor.get().getRealFloat() );
			}
		}
		else
		{
			final Cursor< T > sourceCursor = source.cursor();
			final RandomAccess< FloatType > access = target.randomAccess( target );
			while ( sourceCursor.hasNext() )
			{
				sourceCursor.fwd();
				access.setPosition( sourceCursor );
				access.get().set( sourceCursor.get().getRealFloat() );
			}
		}
		return target;
	}

	private ImgLib2Utils()
	{}

}