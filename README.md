# comp-networks

WebClient is a Java-based application that is designed to handle HTTP and HTTPS requests to download files from specified URLs.

Features:
HTTP and HTTPS Support: The WebClient can process both HTTP and HTTPS requests effectively, making it versatile for various web resources.
File Download: Capable of downloading and saving files from the web, provided the file exists and is accessible through the given URL.
Error Handling: Implements basic error handling for scenarios like malformed URLs and unreachable hosts.
Protocol Flexibility: Automatically adjusts to the specified protocol (HTTP or HTTPS) in the URL without requiring user intervention.

Usage:
To use the WebClient, compile the Java code and run the main class, passing the URL of the file you wish to download as an argument. Example usage from the command line:

java ClientDriver -u [URL]
Replace [URL] with the actual URL of the file you wish to download.

