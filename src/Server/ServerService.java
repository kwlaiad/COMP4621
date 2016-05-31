package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

/**
 * Created by User on 17/5/2016.
 */
public class ServerService extends Thread {

    Socket client;
    boolean gzipEnabled = false;
    static final int defaultChunkedSize = 4096;

    public ServerService(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        String path = "";
        String host = "";
        try {
            ArrayList<String> request = readRequest(client.getInputStream());
            for(int i = 0; i < request.size(); i++) {
                System.out.println(request.get(i));
            }
            // processing request
            if(request.size() == 0) {
                return;
            }
            if(request.get(0).startsWith("GET")) {
                path = request.get(0).split(" ")[1];
            }
            for(int i = 0; i < request.size(); i++) {
                String req = request.get(i);
                if(req.startsWith("Host: ")) {
                    host = req.substring(6);
                }
                if(req.startsWith("Accept-Encoding: ")) {
                    if(req.contains("gzip")) {
                        gzipEnabled = true;
                    }
                }
            }
            if(path.length() == 0 || host.length() == 0) {
                throw new RuntimeException("Request body error!");
            }

            processRequest(path);

            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequest(String path) {
        String type;
        // construct response
        try {
            OutputStream output = client.getOutputStream();
            if (path.equals("/")) {
                path = "/index.html";
            }
            type = setContentType(path);
            switch (type) {
                case "text/html": textResponse(path, type, output);
                    break;
                case "text/css": textResponse(path, type, output);
                    break;
                case "image/jpeg": byteResponse(path, type, output);
                    break;
                case "application/pdf": byteResponse(path, type, output);
                    break;
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation": byteResponse(path, type, output);
                    break;
                default: break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void textResponse(String path, String type, OutputStream output) {
        String header;
        try {
            PrintStream ps = new PrintStream(output);
            File file = new File("./web" + path);
            if (!file.exists()) {
                String error = setErrorMessage(path);
                if (gzipEnabled) {
                    byte[] gzipError = gzipEncoding(error);
                    header = setHttpHeader("404 Not Found", type, "gzip");
                    ps.print(header);
                    chunkedEncoding(gzipError, ps);
                } else {
                    header = setHttpHeader("404 Not Found", type, "");
                    ps.print(header);
                    chunkedEncoding(error, ps);
                }
                return;
            }

            FileInputStream in = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String body = sb.toString();
            if (gzipEnabled) {
                byte[] bo = gzipEncoding(body);
                header = setHttpHeader("200 OK", type, "gzip");
                ps.print(header);
                //ps.write(bo);
                chunkedEncoding(bo, ps);
                //System.out.print(header);
            } else {
                header = setHttpHeader("200 OK", type, "");
                ps.print(header);
                chunkedEncoding(body, ps);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void byteResponse(String path, String type, OutputStream output) {
        String header;
        try {
            PrintStream ps = new PrintStream(output);
            File file = new File("./web" + path);
            if (!file.exists()) {
                String error = setErrorMessage(path);
                if (gzipEnabled) {
                    byte[] gzipError = gzipEncoding(error);
                    header = setHttpHeader("404 Not Found", type, "gzip");
                    ps.print(header);
                    chunkedEncoding(gzipError, ps);
                } else {
                    header = setHttpHeader("404 Not Found", type, "");
                    ps.print(header);
                    chunkedEncoding(error, ps);
                }
                return;
            }

            FileInputStream f = new FileInputStream(file);
            byte[] body = new byte[(int) file.length()];
            f.read(body);

            if (gzipEnabled) {
                byte[] bo = gzipEncoding(body);
                header = setHttpHeader("200 OK", type, "gzip");
                ps.print(header);
                //ps.write(bo);
                chunkedEncoding(bo, ps);
                //System.out.print(header);
            } else {
                header = setHttpHeader("200 OK", type, "");
                ps.print(header);
                chunkedEncoding(body, ps);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> readRequest(InputStream input) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        ArrayList<String> request = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            request.add(line);
            if(line.equals("")) break;
        }

        return request;
    }

    private String setHttpHeader(String status, String type, String contentEncoding) {
        //String header = "HTTP/1.1 " + status + "\r\nContent-Type: "+ type + "\r\nContent-Length: " + length + "\r\nConnection: Keep-Alive\r\n";
        String header = "HTTP/1.1 " + status + "\r\nContent-Type: "+ type + "\r\nTransfer-Encoding: chunked\r\nConnection: Keep-Alive\r\n";

        if(contentEncoding != null) {
            header = header + "Content-Encoding: " + contentEncoding + "\r\n";
        }

        header = header + "\r\n";

        return header;
    }

    private String setErrorMessage(String path) {
        return "<html>\n" +
                "\t<head><title>404 Not Found</title></head>\n" +
                "\t<body><h1>Not Found</h1>\n" +
                "\t<p>The requested URL " + path + " was not found on this server.</p>\n" +
                "\t</body></html>";
    }

    private String setContentType(String path) {
        String type = "text/html";
        if(path.endsWith(".css")) type = "text/css";
        if(path.endsWith(".jpg")) type = "image/jpeg";
        if(path.endsWith(".pdf")) type = "application/pdf";
        if(path.endsWith(".pptx")) type = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

        return type;
    }

    private byte[] gzipEncoding(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);

        gzip.write(data);
        gzip.finish();

        byte[] out = baos.toByteArray();
        baos.close();

        return out;
    }

    private byte[] gzipEncoding(String data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);

        gzip.write(data.getBytes("UTF-8"));
        gzip.finish();

        byte[] out = baos.toByteArray();
        baos.close();

        return out;
    }

    private void chunkedEncoding(byte[] data, PrintStream ps) {
        String len = Integer.toHexString(defaultChunkedSize);
        int count = 0;

        for(; count < data.length;) {
            int sp = data.length - count;
            if(sp < defaultChunkedSize) {
                String len2 = Integer.toHexString(sp);
                ps.print(len2 + "\r\n");
                ps.write(data, count, sp);
                ps.print("\r\n");
            }
            else {
                ps.print(len + "\r\n");
                ps.write(data, count, defaultChunkedSize);
                ps.print("\r\n");
            }
            count += defaultChunkedSize;
        }
        ps.print("0\r\n\r\n");
    }

    private void chunkedEncoding(String data, PrintStream ps) throws UnsupportedEncodingException {
        String len = Integer.toHexString(defaultChunkedSize);
        int count = 0;
        byte[] b = data.getBytes("UTF-8");

        for(; count < b.length;) {
            int sp = b.length - count;
            if(sp < defaultChunkedSize) {
                String len2 = Integer.toHexString(sp);
                ps.print(len2 + "\r\n");
                ps.write(b, count, sp);
                ps.print("\r\n");
            }
            else {
                ps.print(len + "\r\n");
                ps.write(b, count, defaultChunkedSize);
                ps.print("\r\n");
            }
            count += defaultChunkedSize;
        }
        ps.print("0\r\n\r\n");
    }
}
