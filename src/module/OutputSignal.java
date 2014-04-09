package module;
//Wrapper for the type and name of an output signal
//Frankly I guess I don't need the name but yolo
public class OutputSignal {
    public OutputSignal(String name, String t) {
        this.name = name;
        this.type = t;
    }
    String name;
    String type;
}
