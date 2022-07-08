package ServerMano;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

/**
 * localhost:8888/dir1?sort=desc
 * localhost:8888/dir1?sort=asc
 * <p>
 * dir=first
 * dir=last
 */

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
                    if (header.size() > 0) {
                        String[] requestParsed = header.get(0).split(" ");
                        String requestFileName = requestParsed[1];
                        HashMap<String, String> requestParameters = null;
                        if (requestParsed[1].indexOf('?') > 0) {
                            requestParameters = new HashMap<>();
                            String[] requestParsedParsed = requestParsed[1].split("\\?");
                            requestFileName = requestParsedParsed[0];
                            String[] parameters = requestParsedParsed[1].split("&");
                            for (int i = 0; i < parameters.length; i++) {
                                String[] parameterTMP = parameters[i].split("=");
                                requestParameters.put(parameterTMP[0], parameterTMP[1]);
                            }
                        }
                        //   System.out.println(requestFileName);
                        if (requestParsed[0].equals("GET")) {
                            File getFile = new File(ROOT_DIR + requestFileName);
                            String[] parentDirectories = requestFileName.split("/");
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
                                if (requestParameters != null) {
                                    if (requestParameters.containsKey("sort")) {
                                        if (requestParameters.containsKey("dir")) {
                                            files = sortFiles(files, requestParameters.get("sort"), requestParameters.get("dir"));
                                        } else {
                                            files = sortFiles(files, requestParameters.get("sort"), null);
                                        }
                                    }
                                }
                                bufferedWriter.write("<h1>Direktorijos turinys:</h1><pre>\r\n");
                                bufferedWriter.write("<a href=http://localhost:8888/" + parentDir + "> <-Atgal </a> \r\n");
                                for (int i = 0; i < files.length; i++) {
                                    if (files[i].isDirectory()) {
                                        bufferedWriter.write("-DIR- <a href=http://localhost:8888/" + fullDir + files[i].getName() + ">" + files[i].getName() + "</a> \r\n");
                                    } else {
                                        bufferedWriter.write("<a href=http://localhost:8888/" + fullDir + files[i].getName() + ">" + files[i].getName() + "</a> \r\n");
                                    }
                                }
                                bufferedWriter.write("</pre></body></html> \r\n");
                                bufferedWriter.flush();
                            } else if (!getFile.exists()) {
                                ///
                                ///  jei kriepiasi i /servlet bandau iskviesti klase pagal pavadinima
                                ///  ir paduoti jam parametus.
                                ///
                                ///  BetKokiaKlase.getClass.super.MyServlet  <-- parameters or null
                                ///
                                if ("servlet".equals(requestFileName.split("/")[1])) {
                                    if (requestFileName.split("/").length > 2) {
                                        Main main = new Main();
                                        String className = main.getClass().getPackageName() + "." + requestFileName.split("/")[2];
                                        try {
                                            Class<?> clazz = Class.forName(className);
                                            try {
                                                if (requestParameters == null){requestParameters = new HashMap<>();}
                                                MyServlet myServlet = (MyServlet) clazz.getDeclaredConstructor(requestParameters.getClass())
                                                        .newInstance(requestParameters);
                                                if (myServlet.response() != null) {
                                                    bufferedWriter.write(myServlet.response());
                                                } else {
                                                    bufferedWriter.write("HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "\r\n" +
                                                            "<!DOCTYPE html><html><head><title>Servlet klaida</title></head><body>" +
                                                            "<h2>klaida su servletu" + myServlet.getClass().getName()+ " </h2><pre>" +
                                                            "</pre></body></html>");
                                                }
                                                bufferedWriter.flush();
                                            } catch (InvocationTargetException e) {
                                                System.err.println("Klaida  " + e.getMessage());
                                            } catch (InstantiationException e) {
                                                System.err.println("Klaida  " + e.getMessage());
                                            } catch (IllegalAccessException e) {
                                                System.err.println("Klaida  " + e.getMessage());
                                            } catch (NoSuchMethodException e) {
                                                System.err.println("Klaida  " + e.getMessage());
                                            }
                                        } catch (ClassNotFoundException e) {
                                            bufferedWriter.write("HTTP/1.1 404 Not Found\r\n");
                                            bufferedWriter.write("Server: Java MyServer\r\n");
                                            bufferedWriter.write("Content-Type: text/html\r\n");
                                            bufferedWriter.write("Connection: Closed \r\n");
                                            bufferedWriter.write("\r\n");
                                            bufferedWriter.flush();
                                        }
                                    }
                                } else {
                                    bufferedWriter.write("HTTP/1.1 404 Not Found\r\n");
                                    bufferedWriter.write("Server: Java MyServer\r\n");
                                    bufferedWriter.write("Content-Type: text/html\r\n");
                                    bufferedWriter.write("Connection: Closed \r\n");
                                    bufferedWriter.write("\r\n");
                                    bufferedWriter.flush();
                                }
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
                                for (int i = 4; i < uploadData.size() - 2; i++) {
                                    bwUpload.write(uploadData.get(i) + "\r\n");
                                }
                                bwUpload.flush();
                                bwUpload.close();
                                writerUpoload.close();
                                fosUpload.close();
                                bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                                bufferedWriter.write("Content-Type: text/html\r\n");
                                bufferedWriter.write("\r\n");
                                bufferedWriter.write("<html><body>");
                                bufferedWriter.write("<h3>File " + uplodedFileName + " uploded</h3>");
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
                    }
                } catch (IOException ex) {
                    System.out.println("Klaida iostream " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Klaida port " + e.getMessage());
        }
    }

    public static File[] sortFiles(File[] files, String sortMode, String listMode) {
        File[] sortedFiles = new File[files.length];
        ArrayList<File> fileSorted = new ArrayList<>();
        ArrayList<File> dirSorted = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (listMode != null) {
                    if (files[i].isDirectory()) {
                        dirSorted.add(files[i]);
                    } else {
                        fileSorted.add(files[i]);
                    }
                } else {
                    fileSorted.add(files[i]);
                }
            }
        }
        if ("asc".equals(sortMode)) {
            if (listMode != null) {
                Collections.sort(fileSorted);
                Collections.sort(dirSorted);
            } else {
                Collections.sort(fileSorted);
            }
        } else if ("desc".equals(sortMode)) {
            if (listMode != null) {
                Collections.sort(fileSorted, Collections.reverseOrder());
                Collections.sort(dirSorted, Collections.reverseOrder());
            } else {
                Collections.sort(fileSorted, Collections.reverseOrder());
            }
        }
        if ("first".equals(listMode)) {
            dirSorted.addAll(fileSorted);
            for (int i = 0; i < dirSorted.size(); i++) {
                sortedFiles[i] = dirSorted.get(i);
            }
            return sortedFiles;
        } else if ("last".equals(listMode)) {
            fileSorted.addAll(dirSorted);
            for (int i = 0; i < fileSorted.size(); i++) {
                sortedFiles[i] = fileSorted.get(i);
            }
        } else {
            fileSorted.addAll(dirSorted);
            for (int i = 0; i < fileSorted.size(); i++) {
                sortedFiles[i] = fileSorted.get(i);
            }
        }
        return sortedFiles;
    }
}
