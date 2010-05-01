package nl.weeaboo.jdogg.vorbis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggDecoder;

public class VorbisAudioFileReader extends AudioFileReader {

	private static final int HEADER_READ_LIMIT = 64 << 10;
	
	public static final AudioFileFormat.Type VORBIS_TYPE = new AudioFileFormat.Type("Vorbis", "ogg");
	
	public VorbisAudioFileReader() {		
	}
	
	//Functions
	@Override
	public AudioFileFormat getAudioFileFormat(File file)
		throws UnsupportedAudioFileException, IOException
	{
	    InputStream in = new FileInputStream(file);
	    try {	    	
	    	return getAudioFileFormat(in);
	    } finally {
			if (in != null) in.close();
	    }
	}
	
	@Override
	public AudioFileFormat getAudioFileFormat(URL url)
		throws UnsupportedAudioFileException, IOException
	{
	    InputStream in = url.openStream();
	    try {	    	
	    	return getAudioFileFormat(in);
	    } finally {
			if (in != null) in.close();
	    }
	}

	@Override
	public AudioFileFormat getAudioFileFormat(InputStream in)
		throws UnsupportedAudioFileException, IOException
	{				
		in = (!in.markSupported() ? new BufferedInputStream(in) : in);
		
		VorbisDecoder vorbisd = new VorbisDecoder();
		OggDecoder oggd = new OggDecoder();
		oggd.addPacketHandler(OggCodec.Vorbis.id, vorbisd);
		
		if (!readHeaders(oggd, vorbisd, in)) {
			throw new UnsupportedAudioFileException("Unable to read headers from file");
		}
		
		AudioFormat format = vorbisd.getAudioFormat();
		return new AudioFileFormat(VORBIS_TYPE, format, AudioSystem.NOT_SPECIFIED);
	}

	@Override
	public AudioInputStream getAudioInputStream(File file)
		throws UnsupportedAudioFileException, IOException
	{
	    InputStream in = new FileInputStream(file);
	    try {	    	
	    	return getAudioInputStream(in);
	    } finally {
			if (in != null) in.close();
	    }
	}

	@Override
	public AudioInputStream getAudioInputStream(URL url)
		throws UnsupportedAudioFileException, IOException
	{
	    InputStream in = url.openStream();
	    try {	    	
	    	return getAudioInputStream(in);
	    } finally {
			if (in != null) in.close();
	    }
	}

	@Override
	public VorbisAudioInputStream getAudioInputStream(InputStream in)
		throws UnsupportedAudioFileException, IOException
	{
		in = (!in.markSupported() ? new BufferedInputStream(in) : in);

		VorbisDecoder vorbisd = new VorbisDecoder();
		OggDecoder oggd = new OggDecoder();
		oggd.addPacketHandler(OggCodec.Vorbis.id, vorbisd);
		
		if (!readHeaders(oggd, vorbisd, in)) {
			throw new UnsupportedAudioFileException("Unable to read headers from file");
		}
		
		VorbisDecoderInputStream vin = new VorbisDecoderInputStream(in);
		return new VorbisAudioInputStream(vin, vorbisd.getAudioFormat(), AudioSystem.NOT_SPECIFIED);
	}
	
	protected boolean readHeaders(OggDecoder oggd, VorbisDecoder vorbisd, InputStream in)
		throws IOException
	{
		if (!in.markSupported()) {
			throw new IllegalArgumentException("InputStreams passed to this function must support mark.");
		}
		
		in.mark(HEADER_READ_LIMIT + 1);
		
		try {
			oggd.setInputStream(in);
			while (!oggd.isFinished() && !vorbisd.hasReadHeaders()) {
				oggd.update();
			}
		} finally {		
			in.reset();
		}
		
		return vorbisd.hasReadHeaders();
	}
	
}
