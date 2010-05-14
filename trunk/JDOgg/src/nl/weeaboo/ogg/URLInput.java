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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Scanner;

public class URLInput implements RandomOggInput {

	private URL url;
	private int length;
	private long responseTime;
	
	public URLInput(URL u) {
		url = u;
		length = -1;
		responseTime = 10000L;
		
		InputStream in = null;
		try {			
			in = openStream(0, 0);
		} catch (IOException ioe) {
			//Ignore
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException ioe) {
				//Ignore
			}
		}
	}
	
	//Functions
	@Override
	public void close() throws IOException {
	}
	
	@Override
	public InputStream openStream() throws IOException {
		return openStream(0, length());
	}

	@Override
	public InputStream openStream(long off, long len) throws IOException {
		URLConnection con = url.openConnection();
		if (!(con instanceof HttpURLConnection)) {
			//Not a HTTP connection? Time to bail.
			return null;
		}
		
		long t0 = System.currentTimeMillis();
		
		HttpURLConnection urlcon = (HttpURLConnection) con;
		
		//Setup request headers
		urlcon.setRequestProperty("Connection", "Keep-Alive");
		String rangeS = "";
		if (off > 0) rangeS += "bytes=" + off + "-";
		if (len >= 0 && off+len < length) rangeS += len;
		if (rangeS.length() > 0) {
			urlcon.setRequestProperty("Range", rangeS);
		}
		urlcon.setRequestProperty("Content-Type", "application/octet-stream");
		
		//Open connection
		InputStream in = urlcon.getInputStream();

		//Read response header
		rangeS = urlcon.getHeaderField("Content-Range");
		long responseOff = 0;
		long responseEnd = -1;
		if (rangeS != null) {
			Scanner scanner = new Scanner(rangeS);
			scanner.useLocale(Locale.ROOT);
			scanner.skip("bytes\\s*");
			if (scanner.hasNextInt()) {
				responseOff = Math.max(0, scanner.nextInt());
				scanner.skip("\\s*-\\s*");
				if (scanner.hasNextInt()) {
					responseEnd = scanner.nextInt();
				}
			}
		}


		while (responseOff < off && responseOff <= responseEnd) {
			long s = in.skip(off - responseOff);
			if (s <= 0) {
				break;
			}
			responseOff += s;
		}
		
		//position = off;		
		length = urlcon.getHeaderFieldInt("Content-Length", -1);
		
		long respTime = System.currentTimeMillis() - t0;
		if (responseTime < 0) {
			responseTime = respTime;
		} else {
			responseTime = Math.round(0.5 * responseTime + 0.5 * respTime);
		}
		
		return in;
	}

	@Override
	public int read(byte[] b, int off, long foff, int len) throws IOException {
		InputStream in = openStream(foff, len);
		
		int read = 0;
		while (read < len) {
			int r = in.read(b, off + read, len - read);
			if (r < 0) {
				break;
			}
			read += r;
		}
		
		return read;
	}
	
	//Getters
	@Override
	public boolean isReadSlow() {
		return true;
	}
	
	@Override
	public boolean isSeekSlow() {
		return responseTime <= 100;
	}
	
	@Override
	public long length() throws IOException {
		return length;
	}
	
	//Setters
	
}
