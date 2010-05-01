package nl.weeaboo.jdogg.vorbis;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.spi.FormatConversionProvider;

import nl.weeaboo.jdogg.OggCodec;

public class VorbisFormatConversionProvider extends FormatConversionProvider {

	private Encoding sourceEncodings[];
	private Encoding targetEncodings[];
	private VorbisAudioFileReader vafr;
	
	public VorbisFormatConversionProvider() {
		sourceEncodings = new Encoding[] {
			OggCodec.Vorbis.encoding
		};
		targetEncodings = new Encoding[] {
			Encoding.PCM_SIGNED
		};
		
		vafr = new VorbisAudioFileReader();
	}
	
	//Functions
	@Override
	public AudioInputStream getAudioInputStream(Encoding targetEncoding, AudioInputStream ain) {
		AudioFormat fmts[] = getTargetFormats(targetEncoding, ain.getFormat());
		if (fmts.length > 0) {
			return getAudioInputStream(fmts[0], ain);
		}
		throw new IllegalArgumentException("Unsupported audio conversion");
	}

	@Override
	public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream ain) {
		try {
			return vafr.getAudioInputStream(ain);
		} catch (UnsupportedAudioFileException e) {
			throw new IllegalArgumentException("Error getting audio stream: " + e);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error getting audio stream: " + e);
		}		
	}
	
	//Getters
	@Override
	public Encoding[] getSourceEncodings() {
		return sourceEncodings;
	}

	@Override
	public Encoding[] getTargetEncodings() {
		return targetEncodings;
	}

	@Override
	public Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
		return targetEncodings;
	}

	@Override
	public AudioFormat[] getTargetFormats(Encoding targetEncoding, AudioFormat fmt) {
		if (targetEncoding == Encoding.PCM_SIGNED) {
			if (!fmt.getEncoding().equals(OggCodec.Vorbis.encoding)) {
				return new AudioFormat[0];
			}
			
			return new AudioFormat[] {
				new AudioFormat(fmt.getSampleRate(), fmt.getSampleSizeInBits(),
						fmt.getChannels(), true, fmt.isBigEndian())	
			};
		}		
		return new AudioFormat[0];
	}
	
	//Setters
	
}
