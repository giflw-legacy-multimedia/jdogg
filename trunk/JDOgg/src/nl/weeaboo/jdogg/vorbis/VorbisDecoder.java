package nl.weeaboo.jdogg.vorbis;

import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFormat;

import nl.weeaboo.jdogg.AudioException;
import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggPacketHandler;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class VorbisDecoder implements OggPacketHandler {

	private Integer stream;
	private Info info;
	private Comment comment;
	private DspState dspState;
	private Block block;
	private float pcm[][][];
	private int index[];
	private int decodedSamples;
	private boolean hasReadHeaders;
	
	private byte[] temp;
	private ByteArrayOutputStream bout;
	
	public VorbisDecoder() {
	}
	
	//Functions
	@Override
	public void streamOpened(int streamId, int codecId) throws AudioException {
		if (codecId == OggCodec.Vorbis.id) {
			if (stream != null && stream.intValue() != streamId) {
				throw new AudioException(String.format("Too many streams, playing=%d new=%d",
						stream, streamId));
			} else {
				//Start stream
				stream = streamId;
				info = new Info();
				comment = new Comment();
				dspState = new DspState();
				block = new Block(dspState);
				decodedSamples = 0;
				
				temp = new byte[8192];
				bout = new ByteArrayOutputStream();
			}
		}
	}

	@Override
	public void handle(int streamId, Packet packet) throws AudioException {
		if (stream != null && stream.intValue() == streamId) {
			if (!hasReadHeaders) {
				decodedSamples++;
				
				if (info.synthesis_headerin(comment, packet) < 0) {
					throw new AudioException("Error reading headers");
				}
				
				if (decodedSamples == 3) {
					hasReadHeaders = true;
					
					dspState.synthesis_init(info);
					block.init(dspState);
					pcm = new float[1][][];
					index = new int[info.channels];
				}
			} else {
				decode(packet);				
			}
		}
	}
	
	protected void decode(Packet packet) throws AudioException {
		if (block.synthesis(packet) == 0) {
			dspState.synthesis_blockin(block);
		}
		
		int bps = 2; //Bytes per sample
		int ptrinc = info.channels * bps;
		
		int samples;
		while ((samples = dspState.synthesis_pcmout(pcm, index)) > 0) {
			float[][] p = pcm[0];
			int len = samples * info.channels * bps;
			if (temp.length < len) {
				temp = new byte[len];
			}

			for (int ch = 0; ch < info.channels; ch++) {
				int ptr = (ch << 1);
				for (int j = 0; j < samples; j++) {
					int val = (int)(p[ch][index[ch] + j] * 32767f);
					
					if (val > Short.MAX_VALUE) {
						val = Short.MAX_VALUE;
					} else if (val < Short.MIN_VALUE) {
						val = Short.MIN_VALUE;
					}
					
					temp[ptr    ] = (byte)(val & 0xFF);
					temp[ptr + 1] = (byte)((val >> 8) & 0xFF);
					ptr += ptrinc;
				}
			}

			decodedSamples += samples;
			
			bout.write(temp, 0, len);			
			if (dspState.synthesis_read(samples) < 0) {
				break;
			}
		}
	}

	@Override
	public void streamsClosed() {
		stream = null;
	}
	
	//Getters
	public byte[] readDecoded() {
		byte result[] = bout.toByteArray();
		bout.reset();
		return result;
	}
	
	public boolean hasDecoded() {
		return bout != null && bout.size() > 0;
	}
	
	public boolean hasReadHeaders() {
		return hasReadHeaders;
	}
	
	public AudioFormat getAudioFormat() {
		if (!hasReadHeaders) {
			throw new IllegalStateException("Headers not read yet!");
		}
		return new AudioFormat(info.rate, 16, info.channels, true, false);
	}
	
	//Setters
	
}
