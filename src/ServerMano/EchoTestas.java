package ServerMano;

import java.util.HashMap;
import java.util.Map;

public class EchoTestas {
    private final HashMap<String, String> parameters;

    public EchoTestas(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    public String response() {
        String r = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "\r\n" +
                "<!DOCTYPE html><html><head><title>Index</title></head><body>" +
                "<h1>Paduoti parametrai i " + this.getClass().getName() + " klase</h1><pre>";
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            r += entry.getKey() + " => " + entry.getValue() + "\r\n";
        }
        r += "</pre></body></html>";
        return r;
    }
}
