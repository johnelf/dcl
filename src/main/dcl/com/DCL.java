package dcl.com;

import utils.Tools;

import javax.tools.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class DCL {

    public static void main(String [] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = br.readLine();
        while(!input.isEmpty()) {
//            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
//            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
//            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList("/tmp/Tools.java"));
//            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null,
//                    null, compilationUnits);
//            boolean success = task.call();
//            fileManager.close();
            MyClassLoader myClassLoader = new MyClassLoader();
            Tools o = (Tools)myClassLoader.loadClass("utils.Tools").newInstance();
            o.show();
            input = br.readLine();
        }
    }
}
