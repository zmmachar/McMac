//TODO:  Finish creating a useful composition for each spec.  This means using the info in the main module output
//by ptolemy to grab information about which modules are feeding into which others.  First, computer which
//Inputs need to be given by checking which modules have constant arguments.  Use those to compose an argument
//list for the main module.  Then, compute which outputs are actually not being consumed by comparing the 
//list of arguments in main to the list of outputs in each module. Finally, use that list of non-consumed 
//outputs to write a DEFINE section for the main module. Ultimately, create one huge MODULE definition 
//named main with a body, inputs, and outputs properly calculated.
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
        List<ModuleInfo> spec1Modules = getModuleInfo(spec1);
        List<ModuleInfo> spec2Modules = getModuleInfo(spec2);
        
        //Take a list of modules and turn them into one big composite module
        ModuleInfo megaModule1 = makeMegaModule(spec1Modules);
        
        String compositeSMV = getCompositeSMV(spec1Modules, spec2Modules);
        
        try {
            invokeNuSMV(compositeSMV);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return true;
    }
    private ModuleInfo makeMegaModule(List<ModuleInfo> modules) {
        assert(modules.size()>=1);
        //If there is only one module, hooray, just spit that back out
        if(modules.size() == 1) {
            return modules.get(0);
        } else {
            String megaModuleName = "Composite";
            String megaModule = "";
            
            return null;
        }
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
                
            
                //Get name of module, and then its arguments
                
                int argIndex = moduleDeclaration.indexOf('(');
                List<String> args = new ArrayList<String>();
                String moduleName;
                if(argIndex < 0) {
                    moduleName = moduleDeclaration.trim();
                } else {
                    moduleName = moduleDeclaration.substring(0, argIndex);
                    
                    
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
                }
                
                
                //NOW get output signals
                //Which is everything after DEFINE
                int definitionIndex = currentModule.indexOf("DEFINE");
                List<String> outputs = new ArrayList<String>();
                if(definitionIndex < 0 ) {
                     //Do nothing
                } else {
                    String outputBlock= currentModule.substring(currentModule.indexOf("DEFINE"));
                    //Using a scanner to deal with whitespace
                    Scanner outputScanner = new Scanner(outputBlock);
                    String line, outputName;
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
                    outputScanner.close();
                }
                String body = currentModule.substring(newLineIndex);
                modules.add(new ModuleInfo(moduleName, args, outputs, body));
                
            }
        }
                    
        
        return modules;
    }
}
