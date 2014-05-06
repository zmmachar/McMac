//TODO:  Finish creating a useful composition for each spec.  This means using the info in the main module output
//by ptolemy to grab information about which modules are feeding into which others.  First, compute which
//inputs need to be given by checking which modules have unused arguments.  Use those to compose an argument
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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.AbstractMap.SimpleEntry;

import ptolemy.kernel.util.IllegalActionException;
import module.MainModuleInfo;
import module.ModuleInfo;


public class Grader {
    public Grader() {
        
    }
    /**
     * 
     * @param specOne Well formed SMV spec of a FSM
     * @param specTwo Well formed SMV spec of another FSM
     * @return True if the SMVs give equivalent output for equivalent input
     */
    public boolean compareSMVSpecs(String specOne, String specTwo) {
        List<ModuleInfo> specOneModules = getModuleInfo(specOne);
        List<ModuleInfo> specTwoModules = getModuleInfo(specTwo);
        
        //Take a list of modules and turn them into one big composite module
        MainModuleInfo mainModuleOne = makeMegaModule(specOneModules);
        MainModuleInfo mainModuleTwo = makeMegaModule(specTwoModules);
        resolveNamespaceIssues(mainModuleOne, mainModuleTwo);
        
        String compositeSMV = CombineSpecs(mainModuleOne, mainModuleTwo);
        System.out.println(compositeSMV);
        /*
        try {
            invokeNuSMV(compositeSMV);;
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        return true;
    }
    
    private void resolveNamespaceIssues(MainModuleInfo moduleOne, MainModuleInfo moduleTwo) {
        if(moduleOne.name.equals(moduleTwo.name)) {
            //arbitrarily resolve namespace conflict
            moduleTwo.name += "2";
        }
        for(ModuleInfo moduleOneInner: moduleOne.innerModules) {
            for(ModuleInfo moduleTwoInner: moduleTwo.innerModules) {
                if(moduleOneInner.name.equals(moduleTwoInner.name)) {
                    //arbitrarily resolve name space conflict.  I'm assuming people don't have
                    //literally awful naming conventions (dangerous assumption)
                    //( Glass houses, :^] )
                    moduleTwo.refactorModule(moduleTwoInner.name, moduleTwoInner.name + "2");
                }
            }
        }
    }
    
    
    //CONSTRAINT: assuming that all corresponding outputs/arguments HAVE THE SAME NAME
    //CONSTRAINT: assuming NO REUSE of variable names to refer to different variables
    private MainModuleInfo makeMegaModule(List<ModuleInfo> modules) {
        assert(modules.size()>=1);
        //If there is only one module, hooray, just spit that back out
        if(modules.size() == 1) {
            return new MainModuleInfo("compositeModule", modules.get(0).args, modules.get(0).outputs,
                    modules.get(0).body, new ArrayList<ModuleInfo>());
        } else {
            //THIS IS ALL SUPER INEFFICIENT JUST GETTING STUFF DONE
            //Isolate main module (we will be replacing this, but need it for some information)
            ModuleInfo main = null;
            for(ModuleInfo module: modules) {
                if(module.name.equals("main")) {
                    main = module;
                }
            }
            //pull it out from the collection - we don't need to consider it for what follows
            modules.remove(main);
            String megaModuleName = "Composite";
            
            //populate the list of actual outputs
            List<Entry<ModuleInfo, String>>unconsumedOutputs = new ArrayList<Entry<ModuleInfo, String>>();
            //Figure out which outputs are just fed into other modules, and which modules are refinements
            //Assuming refinements do not have unique output
            for(ModuleInfo module: modules) {              
                for(String out: module.outputs) {
                    if(!isArgumentPresent(modules, out) && !isInnerModule(module, modules)) {
                        unconsumedOutputs.add(new SimpleEntry<ModuleInfo, String>(module, out));
                        
                    }    
                }
            }
            
            
            //Top level modules which must be instantiated but need not be fed new variables
            //Are dependent on other declarations
            List<String> topLevelDependentModuleDeclarations = new ArrayList<String>();
            //modules for which we need to provide input.  Will be used to construct a main module
            //argument list
            List<ModuleInfo> topLevelIndependentModules = new ArrayList<ModuleInfo>();
            
            //Going to now assume the format of the (incorrect) main module output by the SMV generator, in order to
            //hackily get at some information about which inputs need to be provided and which inputs checked
            String vars = main.body.substring(main.body.indexOf("VAR"), main.body.indexOf("LTLSPEC"));
            Scanner moduleScanner = new Scanner(vars);
            while(moduleScanner.hasNext()) {
                String line = moduleScanner.nextLine();
                if(line.contains(":")) {
                    String[] assignment = line.split(":");
                    
                    //Format:   SOMETHING: SOMETHING(a, b)
                    String moduleName = assignment[0].trim();
                    String moduleValue = assignment[1].trim();
                    for(ModuleInfo module: modules) {
                        if(module.name.equals(moduleName.trim())) {
                            //Check to see whether this module needs to be manually assigned
                            if(moduleValue.contains("TRUE")) {
                                topLevelIndependentModules.add(module);                               
                            } else {
                                topLevelDependentModuleDeclarations.add(line.trim());
                            }
                            
                        }
                    }
                    
                }
                
            }
            moduleScanner.close();
            
            //Ok, we have all the parts we should need.  Now, piece together frankenstein's monster
            List<String> args = new ArrayList<String>();
            List<String> outputs = new ArrayList<String>();
            String name = "compositeModule";
            String body = "";
            
            for(ModuleInfo module: topLevelIndependentModules) {
                args.addAll(module.args);
            }
            
            for(Entry<ModuleInfo, String> entry: unconsumedOutputs) {
                outputs.add(entry.getValue());
            }
            body += "\n\tVAR\n";
            for(ModuleInfo module: topLevelIndependentModules) {
              body += "\t\t" + module.name + " : " + module.name + module.getArgString() + ";\n";
            }
            for(String moduleDeclaration : topLevelDependentModuleDeclarations) {
                body += "\t\t" + moduleDeclaration + "\n";
            }
            
            body += "\n\n\tDEFINE\n";
            for(Entry<ModuleInfo, String> entry: unconsumedOutputs) {
                body+= "\t\t" + entry.getValue() + " := " + entry.getKey().name + "." + entry.getValue() + ";\n";
            }
            
            MainModuleInfo compositeModule = new MainModuleInfo(name, args, outputs, body, modules);
            //System.out.println(args.toString());
            //System.out.println(outputs.toString());
            
            return compositeModule;
        }
    }
        
    private boolean isInnerModule(ModuleInfo module, List<ModuleInfo> modules) {
        for(ModuleInfo m: modules) {
            if(m.body.contains(module.name + "("))
                return true;
        }
        return false;
    }
    
    private boolean isArgumentPresent(List<ModuleInfo> modules, String output) {
        for(ModuleInfo module: modules) {
            if(module.args.contains(output)) {
                return true;
            }
        }
        return false;
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
    
    private String CombineSpecs(MainModuleInfo mainModuleOne, MainModuleInfo mainModuleTwo) {
      //Only care about the first modules for the demo.  Deal with compositions
        //later
        //Perform very very simple validation on input and output size
        if(mainModuleOne.outputs.size() != mainModuleTwo.outputs.size() ||
                mainModuleTwo.args.size() != mainModuleTwo.args.size())
            return "";
        
        //build composite SMV
        String compositeSMV = "";
        
        compositeSMV += "MODULE main\n\n";
        
        compositeSMV += "\tVAR\n";
        
        //foolishly assume everything is boolean for the sake of simplicity
        for(int i = 0; i < mainModuleOne.args.size(); i++) {
            compositeSMV += "\t\t" + mainModuleOne.args.get(i) + ": boolean;\n";
        }
        compositeSMV += "\t\t" + mainModuleOne.name + ":" + mainModuleOne.name + mainModuleOne.getArgString() + ";\n";
        
        compositeSMV += "\t\t" + mainModuleTwo.name + ":" + mainModuleTwo.name + mainModuleOne.getArgString() + ";\n";
        
        compositeSMV += "\t\tmatch: boolean;\n\n";
        
        compositeSMV += "\tASSIGN\n"
                        + "\t\tinit(match) := TRUE;\n"
                        + "\t\tnext(match) :=\n"
                        + "\t\t\tcase\n\t\t\t\t";
        //Make sure outputs match, assuming the order is identical
        for(int i = 0; i < mainModuleOne.outputs.size(); i++) {
            if(i > 0) 
                compositeSMV += "&";
            
            compositeSMV += mainModuleOne.name +"."+ mainModuleOne.outputs.get(i)
                    + "=" + mainModuleTwo.name + "." + mainModuleTwo.outputs.get(i);
                    
        }
        compositeSMV += ": TRUE;\n"
                + "\t\t\t\tTRUE: FALSE;\n"
                + "\t\t\tesac;\n\n"
                + "\tLTLSPEC\n\n"
                + "\t\tG(match)\n\n";
        
        compositeSMV += mainModuleOne.getModule() + "\n";
        compositeSMV += mainModuleTwo.getModule();
        //System.out.println(compositeSMV);
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
                            args.add(currentArg.trim());
                            currentArg = "";
                        } else {
                            currentArg += moduleDeclaration.charAt(argIndex);
                        }
                        argIndex++;
                    }
                    //Add the final argument
                    args.add(currentArg.trim());
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
                            outputs.add(outputName.trim());
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
