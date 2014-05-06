package grading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;

import ptolemy.verification.kernel.SMVUtility;
import ptolemy.actor.CompositeActor;
import ptolemy.domains.modal.kernel.fmv.FmvAutomaton;
import ptolemy.kernel.Entity;
import ptolemy.moml.MoMLParser;
import ptolemy.verification.kernel.MathematicalModelConverter.FormulaType;

public class GradeTester {
    public static void main(String[] args) throws Exception {
        MoMLParser parser = new MoMLParser();
        CompositeActor actor =(CompositeActor)parser.parseFile("refinementResetTest.xml");
        String spec = SMVUtility.generateSMVDescription(actor, "F(TRUE)", "LTL", "0").toString();
        //System.out.println(spec);
        //System.out.println(spec);
        Grader grader = new Grader();
        grader.compareSMVSpecs(spec, spec);
        /**
        System.out.println(new File(".").getAbsolutePath());
        String spec1, spec2;
        if(args.length == 2) {
            spec1 = getSpecFromFile(args[0]);
            spec2 = getSpecFromFile(args[1]);
        }
        else {
            spec1 = getSpecFromFile("spec.smv");
            spec2 = getSpecFromFile("spec.smv");
        }
        Grader grader = new Grader();
        grader.compareSMVSpecs(spec1, spec2);
        
        **/
        
        /**Iterator actors = automaton.entityList().iterator();
        while(actors.hasNext()) {
            Entity actor = (Entity) actors.next();
            System.out.println(actor);
            if (actor instanceof FmvAutomaton) {
                System.out.println("l");
                System.out.println(((FmvAutomaton)actor).convertToSMVFormat("F(TRUE)", FormulaType.LTL, 0));
            }
        }
        /**
        Runtime rt = Runtime.getRuntime();
       
        String[] cmd = {"NuSMV"};  
        Process pr = rt.exec(cmd);
        InputStreamReader inputStream = new InputStreamReader(
                pr.getInputStream());
        BufferedReader reader = new BufferedReader(inputStream);
        **/
    }
    
    private static String getSpecFromFile(String filename) throws FileNotFoundException {
        File input = new File(filename);
        String spec = "";
        Scanner scan = new Scanner(input);
        while(scan.hasNextLine()) {
            spec += scan.nextLine() + "\n";
        }
        return spec;
    }
}
