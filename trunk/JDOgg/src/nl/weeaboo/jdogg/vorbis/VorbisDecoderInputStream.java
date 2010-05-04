package nl.weeaboo.jdogg.vorbis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import nl.weeaboo.jdogg.OggDecoder;

public class VorbisDecoderInputStream extends InputStream {

	protected final OggDecoder oggDecoder;
	protected final VorbisDecoder vorbisDecoder;
	private ByteBuffer buf;
	
	public VorbisDecoderInputStream(OggDecoder oggd, VorbisDecoder vorbisd) {
		oggDecoder = oggd;
		vorbisDecoder = vorbisd;
		
		buf = ByteBuffer.allocate(0);
	}
	
	//Functions
	@Override
	public int read() throws IOException {
		while (buf.remaining() <= 0) {
			while (!oggDecoder.isEOF() && vorbisDecoder.isBufferEmpty()) {
				oggDecoder.update();
			}
			
			if (oggDecoder.isEOF()) {
				return -1;
			}		
			
			buf = ByteBuffer.wrap(vorbisDecoder.read());
		}
		return (buf.get() & 0xFF);
	}
    
	//Getters
	
	//Setters
	
}
