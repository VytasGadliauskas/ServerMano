package ServerMano;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class Main {
    public static final String ROOT_DIR = "src/ServerMano/resources";
    public static final int PORT = 8888;

    public static void main(String[] args) {
        try (ServerSocket sc = new ServerSocket(PORT)) {
            System.out.println("Server listening on " + PORT);
            File rootDir = new File(ROOT_DIR);
            if (!rootDir.exists()) {
                System.err.println("Root directory " + ROOT_DIR + " do not exist");
            }
            while (true) {
                Socket socket = sc.accept();
                try (InputStream is = socket.getInputStream();
                     Reader reader = new InputStreamReader(is, "UTF-8");
                     BufferedReader bufferedReader = new BufferedReader(reader);
                     OutputStream outputStream = socket.getOutputStream();
                     Writer writer = new OutputStreamWriter(outputStream);
                     BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                    String line;
                    ArrayList<String> header = new ArrayList<>();
                    while ((line = bufferedReader.readLine()) != null && !line.equals("")) {
                        header.add(line);
                    }
                    String[] requestParsed = header.get(0).split(" ");
                    if (requestParsed[0].equals("GET")) {
                        File getFile = new File(ROOT_DIR + requestParsed[1]);
                        String[] parentDirectories = requestParsed[1].split("/");
                        if (getFile.isFile()) {
                            String mimetype = Files.probeContentType(getFile.toPath());
                            try (FileInputStream fis = new FileInputStream(getFile);
                                 Reader r = new InputStreamReader(fis, "UTF-8");
                                 BufferedReader br = new BufferedReader(r)) {
                                if (mimetype != null && mimetype.split("/")[0].equals("image")) {
                                    byte[] imageData = fis.readAllBytes();
                                    bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                                    bufferedWriter.write("Content-Type: image/png\r\n");
                                    bufferedWriter.write("Content-Length: " + imageData.length + "\r\n");
                                    bufferedWriter.write("\r\n");
                                    bufferedWriter.flush();
                                    BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                                    bos.write(imageData);
                                    bos.flush();
                                    bos.close();
                                } else {
                                    bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                                    bufferedWriter.write("Content-Type: text/html\r\n");
                                    bufferedWriter.write("\r\n");
                                    String s;
                                    while ((s = br.readLine()) != null) {
                                        bufferedWriter.write(s + "\r\n");
                                    }
                                    bufferedWriter.flush();
                                }
                            }
                        } else if (getFile.isDirectory()) {
                            String parentDir = "";
                            String fullDir = "";
                            if (parentDirectories.length > 0) {
                                for (int i = 1; i < parentDirectories.length; i++) {
                                    fullDir += parentDirectories[i] + "/";
                                    if (i < parentDirectories.length - 1) {
                                        parentDir += parentDirectories[i] + "/";
                                    }
                                }
                            }
                            bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                            bufferedWriter.write("Content-Type: text/html\r\n");
                            bufferedWriter.write("\r\n");
                            bufferedWriter.write("<html><body>");
                            File[] files = getFile.listFiles();
                            bufferedWriter.write("<h1>Direktorijos turinys:</h1><pre>\r\n");
                            bufferedWriter.write("<a href=http://localhost:8888/" + parentDir + "> <-Atgal </a> \r\n");
                            for (int i = 0; i < files.length; i++) {
                                if (files[i].isDirectory()) {
                                    bufferedWriter.write("DIR <a href=http://localhost:8888/" + fullDir + files[i].getName() + ">" + files[i].getName() + "</a> \r\n");
                                } else {
                                    bufferedWriter.write("<a href=http://localhost:8888/" + fullDir + files[i].getName() + ">" + files[i].getName() + "</a> \r\n");
                                }
                            }
                            bufferedWriter.write("</pre></body></html> \r\n");
                            bufferedWriter.flush();
                        } else if (!getFile.exists()) {
                            bufferedWriter.write("HTTP/1.1 404 Not Found\r\n");
                            bufferedWriter.write("Server: Java MyServer\r\n");
                            bufferedWriter.write("Content-Type: text/html\r\n");
                            bufferedWriter.write("Connection: Closed \r\n");
                            bufferedWriter.write("\r\n");
                            bufferedWriter.flush();
                        }
                    } else if (requestParsed[0].equals("POST")) {
                        ArrayList<String> uploadData = new ArrayList();
                        boolean readUploadBuffer = true;
                        int uploadBufferStartEndMarks = 0;
                        String lineUpload;
                        String uplodedFileName = null;
                        while (readUploadBuffer) {
                            lineUpload = bufferedReader.readLine();
                            uploadData.add(lineUpload);
                            if (lineUpload.length() > 24) {
                                if ("Content-Disposition:".equals(lineUpload.substring(0, 20))) {
                                    String[] lineUploadParsed = lineUpload.split(";");
                                    String fileParameter = (lineUploadParsed[2].split("="))[1];
                                    uplodedFileName = (fileParameter.substring(1, fileParameter.length() - 1));
                                } else if ("------WebKitFormBoundary".equals(lineUpload.substring(0, 24))) {
                                    uploadBufferStartEndMarks++;
                                    if (uploadBufferStartEndMarks == 2) {
                                        readUploadBuffer = false;
                                    }
                                }
                            }
                        }
                        if (uplodedFileName != null) {
                            FileOutputStream fosUpload = new FileOutputStream(ROOT_DIR + "/upload/" + uplodedFileName);
                            Writer writerUpoload = new OutputStreamWriter(fosUpload);
                            BufferedWriter bwUpload = new BufferedWriter(writerUpoload);
                            for (int i = 4; i < uploadData.size()-2; i++) {
                                 bwUpload.write(uploadData.get(i)+"\r\n");
                            }
                            bwUpload.flush();
                            bwUpload.close();
                            writerUpoload.close();
                            fosUpload.close();
                            bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                            bufferedWriter.write("Content-Type: text/html\r\n");
                            bufferedWriter.write("\r\n");
                            bufferedWriter.write("<html><body>");
                            bufferedWriter.write("<h3>File "+uplodedFileName+" uploded</h3>");
                            bufferedWriter.write("</body></html>\r\n");
                            bufferedWriter.flush();
                        }
                    } else {
                        bufferedWriter.write("HTTP/1.1 400 Bad Request\r\n");
                        bufferedWriter.write("Server: Java MyServer\r\n");
                        bufferedWriter.write("Content-Type: text/html\r\n");
                        bufferedWriter.write("Connection: Closed \r\n");
                        bufferedWriter.write("\r\n");
                        bufferedWriter.flush();
                    }
                } catch (IOException ex) {
                    System.out.println("Klaida iostream " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Klaida port " + e.getMessage());
        }
    }

}
