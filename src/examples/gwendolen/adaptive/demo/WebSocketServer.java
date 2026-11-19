// ----------------------------------------------------------------------------
// Copyright (C) 2026 Peter Stringer, Louise A. Dennis
//
// This file is part of the Gwendolen Interpreter.
//
// The Gwendolen Interpreter is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// The Gwendolen Interpreter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with the Gwendolen Interpreter; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------
package gwendolen.adaptive.demo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A minimal WebSocket server (RFC 6455, text frames only) that also serves a single page over
 * HTTP, using only the JDK.  It listens on the loopback interface and only accepts WebSocket
 * connections from pages served by localhost.
 *
 * @author Peter Stringer
 */
public class WebSocketServer {
	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private final int port;
	private final Path page;
	private final Consumer<String> onMessage;
	private final Runnable onConnect;
	private final List<OutputStream> clients = new CopyOnWriteArrayList<OutputStream>();

	/**
	 * @param port the port to listen on.
	 * @param page the page served at /.
	 * @param onMessage called with each text message received from a client.
	 * @param onConnect called when a client connects.
	 */
	public WebSocketServer(int port, Path page, Consumer<String> onMessage, Runnable onConnect) {
		this.port = port;
		this.page = page;
		this.onMessage = onMessage;
		this.onConnect = onConnect;
	}

	/** Start accepting connections on a background thread. */
	public void start() throws IOException {
		ServerSocket server = new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
		Thread t = new Thread(() -> {
			while (true) {
				try {
					Socket s = server.accept();
					Thread c = new Thread(() -> handle(s), "demo-client");
					c.setDaemon(true);
					c.start();
				} catch (IOException e) {
					return;
				}
			}
		}, "demo-server");
		t.setDaemon(true);
		t.start();
	}

	/** Send a text message to every connected client. */
	public void broadcast(String msg) {
		byte[] frame = frame(msg.getBytes(StandardCharsets.UTF_8));
		for (OutputStream o: clients) {
			try {
				synchronized (o) {
					o.write(frame);
					o.flush();
				}
			} catch (IOException e) {
				clients.remove(o);
			}
		}
	}

	private void handle(Socket s) {
		OutputStream out = null;
		try (Socket socket = s) {
			InputStream in = socket.getInputStream();
			out = socket.getOutputStream();
			String request = readHeaders(in);
			String key = header(request, "Sec-WebSocket-Key");
			if (key == null) {
				servePage(request, out);
				return;
			}
			String origin = header(request, "Origin");
			if (origin != null && ! origin.matches("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?")) {
				out.write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
				return;
			}
			String accept = Base64.getEncoder().encodeToString(
					MessageDigest.getInstance("SHA-1").digest((key + GUID).getBytes(StandardCharsets.US_ASCII)));
			out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
					+ "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();
			clients.add(out);
			onConnect.run();
			readFrames(new DataInputStream(in), out);
		} catch (Exception e) {
			// The client went away.
		} finally {
			if (out != null) {
				clients.remove(out);
			}
		}
	}

	private void readFrames(DataInputStream in, OutputStream out) throws IOException {
		ByteArrayOutputStream message = new ByteArrayOutputStream();
		while (true) {
			int b0 = in.readUnsignedByte();
			int b1 = in.readUnsignedByte();
			int opcode = b0 & 0x0F;
			long len = b1 & 0x7F;
			if (len == 126) {
				len = in.readUnsignedShort();
			} else if (len == 127) {
				len = in.readLong();
			}
			byte[] mask = new byte[4];
			if ((b1 & 0x80) != 0) {
				in.readFully(mask);
			}
			byte[] payload = new byte[(int) len];
			in.readFully(payload);
			for (int i = 0; i < payload.length; i++) {
				payload[i] ^= mask[i % 4];
			}
			if (opcode == 8) {
				return;
			} else if (opcode == 9) {
				synchronized (out) {
					out.write(0x8A);
					out.write(payload.length);
					out.write(payload);
					out.flush();
				}
			} else if (opcode == 0 || opcode == 1) {
				message.write(payload);
				if ((b0 & 0x80) != 0) {
					onMessage.accept(message.toString(StandardCharsets.UTF_8));
					message.reset();
				}
			}
		}
	}

	private static byte[] frame(byte[] payload) {
		ByteArrayOutputStream f = new ByteArrayOutputStream();
		f.write(0x81);
		if (payload.length < 126) {
			f.write(payload.length);
		} else if (payload.length < 65536) {
			f.write(126);
			f.write(payload.length >> 8);
			f.write(payload.length);
		} else {
			f.write(127);
			for (int i = 7; i >= 0; i--) {
				f.write((int) ((long) payload.length >> (8 * i)));
			}
		}
		f.write(payload, 0, payload.length);
		return f.toByteArray();
	}

	private void servePage(String request, OutputStream out) throws IOException {
		if (request.startsWith("GET / ") || request.startsWith("GET /index.html ")) {
			byte[] body = Files.readAllBytes(page);
			out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\n"
					+ "Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.write(body);
		} else {
			out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		}
		out.flush();
	}

	private static String readHeaders(InputStream in) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		int c;
		while ((c = in.read()) != -1) {
			b.write(c);
			String s = b.toString(StandardCharsets.US_ASCII);
			if (s.endsWith("\r\n\r\n")) {
				return s;
			}
		}
		throw new IOException("Connection closed");
	}

	private static String header(String request, String name) {
		for (String line: request.split("\r\n")) {
			int i = line.indexOf(':');
			if (i > 0 && line.substring(0, i).trim().equalsIgnoreCase(name)) {
				return line.substring(i + 1).trim();
			}
		}
		return null;
	}
}
