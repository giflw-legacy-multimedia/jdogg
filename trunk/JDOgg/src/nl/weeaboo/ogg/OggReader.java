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

package nl.weeaboo.ogg;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;

public class OggReader {

	private OggInput input;
	
	private PageReader pageReader;
	private List<OggStream> streams; //We'll only have 1-3 streams, Map is overkill
	
	private Page page = new Page();
	private Packet packet = new Packet();

	public OggReader() {
		this(new PageReader());
	}
	public OggReader(PageReader pageReader) {
		this.pageReader = pageReader;
		this.streams = new ArrayList<OggStream>();
	}
	
	//Functions
	protected void reset() {
		pageReader.reset();
		for (OggStream stream : streams) {
			stream.reset();
		}
	}
	
	protected void printStreamState() {
		for (OggStream stream : streams) {
			System.out.printf("id=%d codec=%s endpos=%d\n", stream.getId(),
					String.valueOf(stream.getCodec()), stream.getEndGranulePos());
		}
		System.out.println("----------------------------------------");
	}
	
	protected void init() throws IOException {
		//Read EOS
		if (isSeekable()) {
			readEOS();
			//printStreamState();
		}
				
		//Init BOS
		readBOS();
		//printStreamState();
	}
	
	protected void readBOS() throws IOException {
		reset();
		
		InputStream in = input.openStream();
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}		
		in.mark(64 << 10);
		
		pageReader.setInput(in);
		
		while (!isEOF()) {			
			boolean hasSeenData = false;
			for (OggStream stream : streams) {
				if (stream.hasSeenDataPacket()) {
					hasSeenData = true;
					break;
				}
			}			
			if (hasSeenData) {
				break;
			}

			read();
		}
		
		in.reset();
		reset();
	}
	
	protected void readEOS() throws IOException {
		if (!isSeekable()) {
			throw new IOException("Current OggInput isn't seekable");
		}
		
		reset();
		
		RandomOggInput rinput = (RandomOggInput)input;
		
		int chunkLen = (64 << 10);		
		long len = chunkLen;
		long off = rinput.length() - chunkLen;
		while (off > 0) {			
			int diff = (int)Math.min(off, chunkLen);
			InputStream ein = rinput.openStream(off, len);
			try {
				pageReader.setInput(ein, diff);
				while (!isEOF()) {
					read();
				}
			} finally {
				ein.close();
			}
			
			boolean hasSeenData = false;
			for (OggStream stream : streams) {
				if (stream.hasSeenDataPacket()) {
					hasSeenData = true;
					break;
				}
			}				
			if (hasSeenData) {
				break;
			}

			off -= diff;
			len += diff;
		}
				
		for (OggStream stream : streams) {
			if (!stream.isEOS() && stream.hasSeenDataPacket()) {
				//EOS missing for this stream
				stream.setEndGranulePos(stream.getGranulePos());
			}
		}
	}
	
	public OggStream addStreamHandler(OggStreamHandler<?> h) {
		for (OggStream stream : streams) {
			if (stream.getHandler() == null && stream.getCodec() == h.getCodec()) {
				stream.setHandler(h);
				return stream;
			}
		}
		return null;
	}
	
	public void readStreamHeaders() throws IOException {
		while (!isEOF() && !hasReadHeaders()) {
			read();
		}
		
		if (!hasReadHeaders()) {
			throw new EOFException("EOF before fully reading headers");
		}
	}
	
	public void read() throws IOException {
		if (isEOF()) {
			return;
		}
		
		//Read a page
		if (pageReader.read(page)) {
			int streamId = page.serialno();
			OggStream stream = getStream(streamId);
			if (!stream.isError()) {
				stream.setInput(page);
				while (!stream.isEOS() && stream.read(packet)) {
					//Read another packet
				}
			}
		}
	}
	
	public void seekExact(OggStreamHandler<?> primary, double frac) throws IOException {
		if (!isSeekable()) {
			throw new IOException("Current OggInput isn't seekable");
		}

		double min = 0.0;
		double max = 1.0;
		double guess = frac;
		double endTime = primary.getEndTime();
		double time = frac * endTime;
		
		double lastGuess = 0;
		double lastError = Float.MAX_VALUE;
		
		do {
			//System.out.println(guess + " " + min + " " + max + " " + lastError);
			
			seekApprox(guess);
			while (!isEOF() && !primary.trySync()) {
				read();
			}
			
			double curTime = (primary.getTime() >= 0 ? primary.getTime() : endTime);
			double error = Math.abs(curTime - time);
			if (error < lastError) {
				if (curTime > time) {
					max = guess;
				} else {
					min = guess;
				}
				
				lastError = error;
				lastGuess = guess;
				guess = min + (max - min) / 2; //Binary search
			} else {
				//Error increases or stayed the same, use last estimate.
				guess = lastGuess;
				break;
			}			
		} while (true);
				
		seekApprox(guess);
		while (!isEOF() && !primary.trySync()) {
			read();
		}
		
		//Go forth from the keyframe onward
		RandomOggInput rinput = (RandomOggInput)input;
		if (!rinput.isReadSlow()) {
			while (!isEOF() && !primary.trySkipTo(time)) {
				read();
			}
		}
				
		/*
		boolean ok;
		do {
			ok = true;
			for (OggStream stream : getStreams()) {
				OggStreamHandler<?> h = stream.getHandler();
				if (h != null && !primary.trySkipTo(time)) {
					ok = false;
					break;
				}
			}
			if (!ok) {
				read();
			}
		} while (!ok);
		*/
	}
	public void seekApprox(double frac) throws IOException {
		if (!isSeekable()) {
			throw new IOException("Current OggInput isn't seekable");
		}
		
		long bytepos;
		if (Math.abs(frac) <= 0.000001) {
			bytepos = 0;
		} else if (Math.abs(1.0 - frac) <= 0.000001) {
			bytepos = input.length();
		} else {
			bytepos = Math.round(frac * input.length());
		}
		
		reset();

		RandomOggInput rinput = (RandomOggInput)input;
		pageReader.setInput(rinput.openStream(bytepos, input.length() - bytepos));
	}
	
	//Getters
	public OggStream getStream(int id) {
		for (OggStream stream : streams) {
			if (stream.getId() == id) {
				return stream;
			}
		}
		
		OggStream stream = new OggStream(id);
		streams.add(stream);
		return stream;
	}
	
	public boolean hasReadHeaders() {
		for (OggStream stream : streams) {
			if (stream.getHandler() != null) {
				if (!stream.getHandler().hasReadHeaders()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public Collection<OggStream> getStreams() {
		return streams;
	}
	
	public boolean isSeekable() {
		return input instanceof RandomOggInput;
	}
	
	public boolean isSeekSlow() {
		if (input instanceof RandomOggInput) {
			RandomOggInput rinput = (RandomOggInput)input;
			return rinput.isSeekSlow();
		}
		return true;
	}
	
	public boolean isEOF() {
		return pageReader.isEOF();
	}
	
	//Setters
	public void setInput(OggInput in) throws IOException {
		input = in;
		
		init();
	}
	
}
