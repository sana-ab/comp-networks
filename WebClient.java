
/**
 * WebClient Class
 *
 * CPSC 441
 * Assignment 1
 *
 * @author 	Majid Ghaderi
 * @version	2024
 *
 */

import java.io.*;
import java.util.logging.*;
import java.net.Socket;
import java.net.MalformedURLException;
import javax.net.ssl.SSLSocketFactory;

public class WebClient {

    private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    /**
     * Default no-arg constructor
     */
    public WebClient() {
        // nothing to do!
    }

    /**
     * Downloads the object specified by the parameter url.
     *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
    public void getObject(String url) {
        String protocol = "";
        String hostname = "";
        int port = -1;
        String pathname = "";

        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Parse the URL and extract parts such as protocol, hostname, and pathname
            URLParts urlParts = parseURL(url);
            protocol = urlParts.protocol;
            hostname = urlParts.hostname;
            port = urlParts.port;
            pathname = urlParts.pathname;
            // Determine if the protocol is HTTP or HTTPS, and establish the appropriate connection
            if (protocol.equalsIgnoreCase("HTTP")) {
                socket = new Socket(hostname, port);
            } else if (protocol.equalsIgnoreCase("HTTPS")) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(hostname, port);
            } else {
                System.out.println("Unsupported protocol: " + protocol);
                return;
            }

            // Construct and send the GET request
            String request = constructGETRequest(hostname, pathname);
            outputStream = socket.getOutputStream();
            outputStream.write(request.getBytes("US-ASCII"));
            outputStream.flush();

            // Print the HTTP request
            System.out.println(request.trim());

            // Read the HTTP response
            inputStream = socket.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            HttpResponse response = readHttpResponse(bufferedInputStream);

            // Print the status line and headers (only once)
            System.out.println(response.statusLine);
            System.out.println(response.headers);

            // If the status code is 200 (OK), download the file content
            if (response.statusCode == 200) {
                String filename = getFileNameFromPath(pathname);
                FileOutputStream fileOutputStream = new FileOutputStream(filename);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                fileOutputStream.close(); // Close the file output stream
            }

        } catch (IOException e) {
            // Handle any network-related errors that occur during connection or I/O operations
            System.out.println("Network error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Ensure that all resources (streams and socket) are closed properly
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Represents the components of a URL such as protocol, hostname, port, and pathname.
     */
    private class URLParts {
        String protocol;
        String hostname;
        int port;
        String pathname;

        /**
         * Constructor to create URLParts.
         *
         * @param protocol  The protocol (HTTP or HTTPS) of the URL.
         * @param hostname  The hostname of the server.
         * @param port      The port number (80 for HTTP, 443 for HTTPS).
         * @param pathname  The path part of the URL (e.g., /index.html).
         */
        URLParts(String protocol, String hostname, int port, String pathname) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.port = port;
            this.pathname = pathname;
        }
    }
    /**
     * Parses a given URL into its components (protocol, hostname, port, and pathname).
     *
     * @param url  A fully qualified URL to be parsed.
     * @return     A URLParts object containing the protocol, hostname, port, and pathname.
     * @throws MalformedURLException If the URL is in an invalid format.
     */
    private URLParts parseURL(String url) throws MalformedURLException {
        String protocol = "";
        String hostname = "";
        int port = -1;
        String pathname = "";

        int protocolEndIndex = url.indexOf("://");
        if (protocolEndIndex == -1) {
            throw new MalformedURLException("Invalid URL format");
        }
        protocol = url.substring(0, protocolEndIndex);
        String rest = url.substring(protocolEndIndex + 3);

        int pathStartIndex = rest.indexOf("/");
        String hostPort = pathStartIndex == -1 ? rest : rest.substring(0, pathStartIndex);
        pathname = pathStartIndex == -1 ? "/" : rest.substring(pathStartIndex);

        int portIndex = hostPort.indexOf(":");
        if (portIndex == -1) {
            hostname = hostPort;
            port = protocol.equalsIgnoreCase("HTTP") ? 80 : 443;
        } else {
            hostname = hostPort.substring(0, portIndex);
            port = Integer.parseInt(hostPort.substring(portIndex + 1));
        }

        return new URLParts(protocol, hostname, port, pathname);
    }

    /**
     * Constructs an HTTP GET request string for the specified hostname and pathname.
     *
     * @param hostname  The server's hostname.
     * @param pathname  The resource's pathname on the server.
     * @return          A string representing the HTTP GET request.
     */
    private String constructGETRequest(String hostname, String pathname) {
        return "GET " + pathname + " HTTP/1.1\r\n" +
                "Host: " + hostname + "\r\n" +
                "Connection: close\r\n\r\n";
    }

    /**
     * Represents an HTTP response, including the status line, headers, and content length.
     */
    private class HttpResponse {
        int statusCode;
        String headers;
        String statusLine;
        int contentLength;

        /**
         * Constructor for HttpResponse.
         *
         * @param statusCode     The HTTP status code (e.g., 200 for OK).
         * @param headers        The headers of the HTTP response.
         * @param statusLine     The status line of the HTTP response.
         * @param contentLength  The length of the content in the response body.
         */
        HttpResponse(int statusCode, String headers, String statusLine, int contentLength) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.statusLine = statusLine;
            this.contentLength = contentLength;
        }
    }

    /**
     * Reads and parses the HTTP response from the server, extracting the status line,
     * headers, and content length.
     *
     * @param inputStream  A BufferedInputStream to read the HTTP response from.
     * @return             An HttpResponse object containing the status line, headers, and content length.
     * @throws IOException If an I/O error occurs during reading.
     */
    private HttpResponse readHttpResponse(BufferedInputStream inputStream) throws IOException {
        StringBuilder headers = new StringBuilder();
        String line;
        String statusLine = readLine(inputStream);
        int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
        int contentLength = -1;

        while (!(line = readLine(inputStream)).isEmpty()) {
            headers.append(line).append("\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }

        return new HttpResponse(statusCode, headers.toString(), statusLine, contentLength);
    }

    /**
     * Reads a single line from the input stream, where a line ends with \r\n.
     *
     * @param inputStream  The input stream to read from.
     * @return             A string representing the line read.
     * @throws IOException If an I/O error occurs during reading.
     */
    private String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int previous = 0, current;
        while ((current = inputStream.read()) != -1) {
            lineBuffer.write(current);
            if (previous == '\r' && current == '\n') {
                break;
            }
            previous = current;
        }
        return lineBuffer.toString("US-ASCII").trim();
    }


    /**
     * Extracts the file name from a given URL path.
     *
     * This method determines the appropriate file name to use when saving the downloaded
     * object. If the pathname ends with a "/", it assumes the file is an index file
     * ("index.html"). Otherwise, it extracts the file name from the last segment
     * of the URL path.
     *
     * @param pathname  The URL path from which to extract the file name.
     * @return          The extracted file name. If the path ends with a "/",
     *                  returns "index.html".
     */
    private String getFileNameFromPath(String pathname) {
        return pathname.endsWith("/") ? "index.html" : pathname.substring(pathname.lastIndexOf("/") + 1);
    }
}
