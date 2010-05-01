package nl.weeaboo.jdogg;

/* 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Street #330, Boston, MA 02111-1307, USA.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;

public class OggDecoder {
	
	private static final int BUFFSIZE = 8192;

	private InputStream inputStream;
	private SyncState syncState;
	private boolean finished;
	private Map<Integer, OggStream> streams;
	private Map<Integer, List<OggPacketHandler>> packetHandlers;
	
	private Thread thread;
	private volatile boolean stop;

	public OggDecoder() {
		packetHandlers = new HashMap<Integer, List<OggPacketHandler>>();
	}

	public synchronized void start(InputStream in) {
		stop();
		
		setInputStream(in);
		
		Runnable r = new Runnable() {
			public void run() {
				while (!stop && !isFinished()) {
					try {
						update();
					} catch (Throwable t) {
						t.printStackTrace();
						stop = true;
					}
				}
			}
		};
		
		thread = new Thread(r);
		thread.start();
	}

	public synchronized void stop() {
		stop = true;
		
		waitFor();
	}
	
	public synchronized void waitFor() {
		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) { }
		
			if (!isFinished()) {
				finished();
			}
			
			thread = null;
		}
	}
	
	public void update() throws IOException {
		Page page = new Page();
		Packet packet = new Packet();

		//Read some data
		int index = syncState.buffer(BUFFSIZE);
		int read = inputStream.read(syncState.data, index, BUFFSIZE);		
		if (read < 0) {
			finished();
			return;
		}
		
		syncState.wrote(read);

		//Process the data we just read
		while (!stop) {
			int pageoutRes = syncState.pageout(page);			
			if (pageoutRes < 0) {
				finished();
				break; //Error
			} else if (pageoutRes == 0) {
				break; //Need more data
			}
			
			//Get the correct stream by ID
			int streamId = page.serialno();
			OggStream stream = streams.get(streamId);				
			if (stream == null) {
				stream = new OggStream(streamId);
				streams.put(streamId, stream);
			}
			
			int pageinRes = stream.ss.pagein(page);
			if (pageinRes < 0) {
				throw new IOException("Error reading first page of Ogg bitstream data.");
			}
			
			Collection<OggPacketHandler> phs = packetHandlers.get(stream.type);			
			while (!stop) {
				int packetoutRes = stream.ss.packetout(packet);
				if (packetoutRes < 0) {
					finished();
					break; //Error
				} else if (packetoutRes == 0) {
					break; //Need more data
				}

				//Process packet
				if (stream.type == 0) {
					stream.type = packet.packet_base[packet.packet + 1];
					
					phs = packetHandlers.get(stream.type);					
					if (phs != null) {
						for (OggPacketHandler ph : phs) {
							ph.streamOpened(streamId, stream.type);
						}
					}
				}
				
				if (phs != null) {
					for (OggPacketHandler ph : phs) {
						ph.handle(streamId, packet);
					}
				}
			}
		}
	}
	
	public synchronized void addPacketHandler(int codecId, OggPacketHandler ph) {
		List<OggPacketHandler> l = packetHandlers.get(codecId);
		if (l == null) {
			l = new CopyOnWriteArrayList<OggPacketHandler>();
			packetHandlers.put(codecId, l);
		}
		l.add(ph);
	}	
	public synchronized void removePacketHandler(OggPacketHandler ph) {
		for (Collection<OggPacketHandler> phs : packetHandlers.values()) {
			phs.remove(ph);
		}
	}
	
	protected void finished() {
		if (!finished) {
			finished = true;
			
			for (Collection<OggPacketHandler> phs : packetHandlers.values()) {
				for (OggPacketHandler ph : phs) {
					ph.streamsClosed();
				}
			}
		}
	}
	
	//Getters
	public boolean isFinished() {
		return finished;
	}
	
	//Setters
	public void setInputStream(InputStream in) {
		if (inputStream != in) {
			inputStream = in;
			syncState = new SyncState();
			finished = false;
			streams = new HashMap<Integer, OggStream>();
			
			stop = false;
		}
	}
	
	//Inner Classes
	private static class OggStream {
		
		public final StreamState ss;
		public int type;

		public OggStream(int serial) {
			ss = new StreamState();
			ss.init(serial);
			ss.reset();
		}
	}
	
}
