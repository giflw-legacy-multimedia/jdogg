package nl.weeaboo.jdogg.vorbis;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class VorbisAudioInputStream extends AudioInputStream {

	public VorbisAudioInputStream(VorbisDecoderInputStream in,
			AudioFormat format, long length)
	{
		super(in, format, length);
	}
	
	//Functions
    
	//Getters
	
	//Setters
	
}
