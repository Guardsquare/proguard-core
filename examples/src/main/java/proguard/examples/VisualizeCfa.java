package proguard.examples;

import static proguard.analysis.cpa.jvm.util.CfaUtil.createInterproceduralCfaFromClassPool;
import static proguard.examples.JarUtil.readJar;

import java.io.IOException;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.classfile.ClassPool;

/**
 * This sample application produces a DOT graph representation of a Control Flow Automaton generated
 * by ProGuardCORE.
 *
 * <p>Usage: java proguard.examples.VisualizeCfa input.jar
 *
 * <p>where the input can be a jar file or a directory containing jar files.
 *
 * @author James Hamilton
 */
public class VisualizeCfa {
  public static void main(String[] args) throws IOException {
    ClassPool programClassPool = readJar(args[0], false);
    JvmCfa cfa = createInterproceduralCfaFromClassPool(programClassPool);
    System.out.println(CfaUtil.toDot(cfa));
  }
}
