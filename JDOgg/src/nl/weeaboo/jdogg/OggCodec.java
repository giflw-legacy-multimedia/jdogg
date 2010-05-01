package nl.weeaboo.jdogg;

import javax.sound.sampled.AudioFormat;

public enum OggCodec {
	Vorbis(0x76), Theora(0x74);
	
	public final int id;
	public final AudioFormat.Encoding encoding;
	
	private OggCodec(int id) {
		this.id = id;
		this.encoding = new AudioFormat.Encoding(toString());
	}
	
	public static OggCodec fromInt(int id)  {
		for (OggCodec codec : values()) {
			if (codec.id == id) {
				return codec;
			}
		}
		return null;
	}
}
