package ServerMano;

import java.util.HashMap;

abstract public class MyServlet {
    protected final HashMap<String, String> parameters;

    public MyServlet(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    abstract public String response();
}
