package ServerMano;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class EchoTestas extends MyServlet{
    public EchoTestas(HashMap<String, String> parameters) {
        super(parameters);
    }

    public String response() {
        String r = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "\r\n" +
                "<!DOCTYPE html><html><head><title>Index</title></head><body>" +
                "<h2>EchoTestas servletas " + this.getClass().getName() + " </h2><pre>";
        for (Map.Entry<String, String> entry : super.parameters.entrySet()) {
            r += entry.getKey() + " => " + entry.getValue() + "\r\n";
        }
        r += "</pre></body></html>";
        return r;
    }
}
