package module;

import java.util.List;

//Basically a wrapper class with info necessary to replicate an SVM module
public class ModuleInfo {
   
    public String name;
    public List<String> args;
    public List<String> outputs;
    public String body;
    
    public ModuleInfo(String moduleName, List<String> parameters, List<String> retVals,
            String body) {
        this.name = moduleName;
        this.args = parameters;
        this.outputs = retVals;
        this.body = body;
    }
    
    public String getArgString() {
        String argString = "(";
        for(int i = 0; i < args.size()-1; i++)
            argString += args.get(i) + ",";
        argString += args.get(args.size()-1) + ")";
        return argString;
    }
    
    public String getModule() {
        String module = "";
        module += "MODULE " + name + getArgString() + "\n";
        module += body;
        return module;
    }
}
