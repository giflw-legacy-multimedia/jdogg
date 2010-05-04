package nl.weeaboo.jdogg;

public class GreedyStreamSelector implements StreamSelectorListener {

	private OggStreamHandler handlers[];
	private boolean used[];
	
	public GreedyStreamSelector(OggStreamHandler... hs) {
		handlers = hs;
		used = new boolean[handlers.length];
	}
	
	//Functions
	public void onSealed(StreamGroup group) {
		for (OggStream stream : group.getStreams()) {
			for (int n = 0; n < handlers.length; n++) {
				if (used[n]) continue;
				
				if (stream.getCodec() == handlers[n].getCodec()) {
					used[n] = true;
					stream.setHandler(handlers[n]);
					
					OggDecoder oggd = group.getOggDecoder();
					if (oggd != null && oggd.getPrimaryStream() == null) {
						oggd.setPrimaryStream(stream);
					}
				}
			}
		}
	}
	
	//Getters
	
	//Setters
	
}
