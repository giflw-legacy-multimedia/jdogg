package nl.weeaboo.jdogg.vorbis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggDecoder;

public class VorbisDecoderInputStream extends InputStream {

	protected final OggDecoder oggDecoder;
	protected final VorbisDecoder vorbisDecoder;
	private ByteBuffer buf;
	
	public VorbisDecoderInputStream(InputStream in) {
		vorbisDecoder = new VorbisDecoder();
		
		oggDecoder = new OggDecoder();
		oggDecoder.addPacketHandler(OggCodec.Vorbis.id, vorbisDecoder);
		oggDecoder.setInputStream(in);
		
		buf = ByteBuffer.allocate(0);
	}
	
	//Functions
	@Override
	public int read() throws IOException {
		if (buf.remaining() <= 0) {
			while (!oggDecoder.isFinished() && !vorbisDecoder.hasDecoded()) {
				oggDecoder.update();
			}
			
			if (oggDecoder.isFinished()) {
				return -1;
			}		
			
			buf = ByteBuffer.wrap(vorbisDecoder.readDecoded());
		}
		return (buf.get() & 0xFF);
	}
    
	//Getters
	
	//Setters
	
}
