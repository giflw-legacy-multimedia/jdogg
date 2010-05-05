/* JDOgg
 * 
 * Copyright (c) 2010 Timon Bijlsma
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package nl.weeaboo.ogg.vorbis;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.spi.FormatConversionProvider;

import nl.weeaboo.ogg.OggCodec;

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
