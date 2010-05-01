package nl.weeaboo.jdogg;

import com.jcraft.jogg.Packet;

public interface OggPacketHandler {

	public void streamOpened(int streamId, int codecId) throws AudioException;
	public void handle(int streamId, Packet packet) throws AudioException;
	public void streamsClosed();
	
}
