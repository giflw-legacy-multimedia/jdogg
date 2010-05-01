package nl.weeaboo.jdogg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.weeaboo.jdogg.vorbis.VorbisDecoderInputStream;

public class OggTest {

	//Functions
	public static void main(String args[]) {		
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream("test.ogv"));

			/*
			OggPacketHandler vorbisHandler = new VorbisDecoder();
			OggDecoder decoder = new OggDecoder();
			decoder.addPacketHandler(OggCodec.Vorbis.getId(), vorbisHandler);			
			decoder.start(in);
			decoder.waitFor();
			*/
			
			VorbisDecoderInputStream vin = new VorbisDecoderInputStream(in);
			
			BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream("test.pcm"));
			int read;
			while ((read = vin.read()) >= 0) {
				bout.write(read);
			}
			bout.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException ioe) { }
		}
	}
	
	//Getters
	
	//Setters
	
}
