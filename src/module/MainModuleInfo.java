package module;

import java.util.List;

public class MainModuleInfo extends ModuleInfo {
    
    public List<ModuleInfo> innerModules;
    
    public MainModuleInfo(String moduleName, List<String> parameters,
            List<String> retVals, String body, List<ModuleInfo> innerModules) {
        super(moduleName, parameters, retVals, body);
        this.innerModules = innerModules;
    }

    
    public boolean containsModuleName(String name) {
        for(ModuleInfo module:innerModules) {
            if(module.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public void refactorModule(String oldName, String newName) {
        ModuleInfo module = null;
        for(ModuleInfo m : innerModules) {
            if(m.name.equals(oldName)) {
                module = m;
                break;
            }
        }
        if(module == null) {
            System.out.println("No such module: " + oldName);
            return;
        } else {
            module.name = newName;
            for(ModuleInfo m: innerModules) {
                m.body = m.body.replace(oldName, newName);
                //JUST IN CASE one name is a subset of another, shouldn't matter but
                //for consistency need to do partial replacement everywhere
                
                m.name = (m.name.equals(newName))?  m.name : m.name.replace(oldName, newName) ;
            }
        }
        this.body = this.body.replace(oldName, newName);
    }
    
    public String getModule() {
        String moduleDescription = super.getModule();
        for(ModuleInfo module: innerModules) {
            moduleDescription += "\n" + module.getModule();
        }
        return moduleDescription;
    }
    
    
}
