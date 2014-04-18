package grading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import ptolemy.kernel.util.IllegalActionException;
import module.ModuleInfo;


public class Grader {
    public Grader() {
        
    }
    /**
     * 
     * @param spec1 Well formed SMV spec of a FSM
     * @param spec2 Well formed SMV spec of another FSM
     * @return True if the SMVs give equivalent output for equivalent input
     */
    public boolean compareSMVSpecs(String spec1, String spec2) {
        //System.out.println(spec1);
        List<ModuleInfo> spec1Modules = getModuleInfo(spec1);
        List<ModuleInfo> spec2Modules = getModuleInfo(spec2);
        
        String compositeSMV = getCompositeSMV(spec1Modules, spec2Modules);
        
        try {
            invokeNuSMV(compositeSMV);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return true;
    }
    
    private void invokeNuSMV(String spec) throws IOException {
        // Establish the file.
        StringBuffer buffer = new StringBuffer();
        File smvFile = new File("Composite.smv");
        String fileAbsolutePath = smvFile.getAbsolutePath();

        FileWriter writer = null;
        try {
            writer = new FileWriter(smvFile);
            writer.write(spec);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        BufferedReader reader = null;
        
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("NuSMV " + "\""
                    + fileAbsolutePath + "\"");
            InputStreamReader inputStream = new InputStreamReader(
                    pr.getInputStream());
            reader = new BufferedReader(inputStream);
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

        } finally {
            reader.close();
        }
        //smvFile.delete();
        System.out.println(buffer);
        
    }
    
    private String getCompositeSMV(List<ModuleInfo> spec1Modules, List<ModuleInfo> spec2Modules) {
      //Only care about the first modules for the demo.  Deal with compositions
        //later
        ModuleInfo module1 = spec1Modules.get(0);
        ModuleInfo module2 = spec2Modules.get(0);
        //Perform very very simple validation on input and output size
        if(module1.outputs.size() != module2.outputs.size() ||
                module2.args.size() != module2.args.size())
            return "";
        //Avoid module namespace issues in a hacky way
        if(module1.name.equals(module2.name))
            module2.name = module2.name + "2";
        
        //build composite SMV
        String compositeSMV = "";
        
        compositeSMV += "MODULE main\n\n";
        
        compositeSMV += "\tVAR\n";
        
        //foolishly assume everything is boolean for the sake of simplicity
        for(int i = 0; i < module1.args.size(); i++) {
            compositeSMV += "\t\t" + module1.args.get(i) + ": boolean;\n";
        }
        
        compositeSMV += "\t\t" + module1.name + ":" + module1.name + module1.getArgString() + ";\n";
        
        compositeSMV += "\t\t" + module2.name + ":" + module2.name + module1.getArgString() + ";\n";
        
        compositeSMV += "\t\tmatch: boolean;\n\n";
        
        compositeSMV += "\tASSIGN\n"
                        + "\t\tinit(match) := TRUE;\n"
                        + "\t\tnext(match) :=\n"
                        + "\t\t\tcase\n\t\t\t\t";
        //Make sure outputs match, assuming the order is identical
        for(int i = 0; i < module1.outputs.size(); i++) {
            if(i > 0) 
                compositeSMV += "&";
            
            compositeSMV += module1.name +"."+ module1.outputs.get(i)
                    + "=" + module2.name + "." + module2.outputs.get(i);
                    
        }
        compositeSMV += ": TRUE;\n"
                + "\t\t\t\tTRUE: FALSE;\n"
                + "\t\t\tesac;\n\n"
                + "\tLTLSPEC\n\n"
                + "\t\tG(match)\n\n";
        
        compositeSMV += module1.getModule() + "\n";
        compositeSMV += module2.getModule();
        System.out.println(compositeSMV);
        return compositeSMV;
    }
    
    
    
    
    
    //Assuming for the moment that there is one module per SMV spec
    private List<ModuleInfo> getModuleInfo(String smvSpec) {
        String currentSpec = smvSpec;
        List<ModuleInfo> modules = new ArrayList<ModuleInfo>();
        while(currentSpec.contains("MODULE")) {
            int moduleIndex = currentSpec.indexOf("MODULE");
            if(moduleIndex != -1) {
                //Get the entire module out
                int moduleEnding = currentSpec.substring(moduleIndex+6).indexOf("MODULE");
                //If you are on the last module in the file, just grab all till the end
                moduleEnding = (moduleEnding == -1)? currentSpec.length() : moduleEnding+6;
                
                String currentModule = currentSpec.substring(moduleIndex, moduleEnding);
                
                int newLineIndex = currentModule.indexOf("\n");
                
                //Get the declaration
                String moduleDeclaration = currentModule.substring(6, newLineIndex).trim();
                
                currentSpec = currentSpec.substring(moduleEnding);
                
                if (moduleDeclaration.equals("main")) {
                    System.out.println("Ignoring main module");
                    //Look for the non main modules
                } else {
                    //Get name of module, and then its arguments
                    
                    int argIndex = moduleDeclaration.indexOf('(');
                    
                    String moduleName = moduleDeclaration.substring(0, argIndex);
                    
                    List<String> args = new ArrayList<String>();
                    
                    //Parse list of arguments to method;
                    String currentArg = "";
                    argIndex++;
                    //ignore trailing paren
                    while(argIndex < moduleDeclaration.length() - 1) {
                        if(moduleDeclaration.charAt(argIndex) == ',') {
                            args.add(currentArg);
                            currentArg = "";
                        } else {
                            currentArg += moduleDeclaration.charAt(argIndex);
                        }
                        argIndex++;
                    }
                    //Add the final argument
                    args.add(currentArg);
                    
                    
                    //NOW get output signals
                    //Which is everything after DEFINE
                    String outputBlock= currentModule.substring(currentModule.indexOf("DEFINE"));
                    //Using a scanner to deal with whitespace
                    Scanner outputScanner = new Scanner(outputBlock);
                    List<String> outputs = new ArrayList<String>();
                    String line, outputName, outputType;
                    int assignIndex;
                    while(outputScanner.hasNext()) {
                        line = outputScanner.nextLine();
                        assignIndex = line.indexOf(":=");
                        if(assignIndex > -1) {
                            //if there is an assignment, there is an output var
                            outputName = line.substring(0,assignIndex).trim();
                            outputs.add(outputName);
                        }
                    }
                    String body = currentModule.substring(newLineIndex);
                    modules.add(new ModuleInfo(moduleName, args, outputs, body));
                    
                }
            }
                    
        }
        return modules;
    }
}
