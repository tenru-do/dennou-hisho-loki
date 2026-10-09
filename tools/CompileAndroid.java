import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.tools.*;

/** Supplies readable Android API classes without mounting a ZipFS on the jar. */
public final class CompileAndroid {
    static final class ApiClass extends SimpleJavaFileObject {
        final ZipFile archive; final ZipEntry entry; final String binaryName;
        ApiClass(ZipFile archive, ZipEntry entry) {
            super(URI.create("android-api:///" + entry.getName()), Kind.CLASS);
            this.archive=archive; this.entry=entry;
            this.binaryName=entry.getName().substring(0,entry.getName().length()-6).replace('/','.');
        }
        public InputStream openInputStream() throws IOException { return archive.getInputStream(entry); }
    }
    static final class ApiManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        final Map<String,List<ApiClass>> packages=new HashMap<>();
        final Map<String,ApiClass> classes=new HashMap<>();
        ApiManager(StandardJavaFileManager delegate, ZipFile archive) {
            super(delegate);
            Enumeration<? extends ZipEntry> entries=archive.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement();
                if(entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
                    ApiClass cls=new ApiClass(archive,entry); classes.put(cls.binaryName,cls);
                    int last=cls.binaryName.lastIndexOf('.');
                    String pkg=last<0?"":cls.binaryName.substring(0,last);
                    packages.computeIfAbsent(pkg,k->new ArrayList<>()).add(cls);
                }
            }
        }
        public Iterable<JavaFileObject> list(Location location, String pkg, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
            if(location==StandardLocation.CLASS_PATH) {
                List<JavaFileObject> result=new ArrayList<>();
                if(kinds.contains(JavaFileObject.Kind.CLASS)) {
                    if(!recurse) result.addAll(packages.getOrDefault(pkg,Collections.emptyList()));
                    else for(Map.Entry<String,List<ApiClass>> p:packages.entrySet())
                        if(p.getKey().equals(pkg) || p.getKey().startsWith(pkg+".")) result.addAll(p.getValue());
                }
                return result;
            }
            return super.list(location,pkg,kinds,recurse);
        }
        public String inferBinaryName(Location location, JavaFileObject file) {
            return file instanceof ApiClass ? ((ApiClass)file).binaryName : super.inferBinaryName(location,file);
        }
        public JavaFileObject getJavaFileForInput(Location location,String name,JavaFileObject.Kind kind) throws IOException {
            if(location==StandardLocation.CLASS_PATH && kind==JavaFileObject.Kind.CLASS) return classes.get(name);
            return super.getJavaFileForInput(location,name,kind);
        }
        public boolean contains(Location location,FileObject file) throws IOException {
            if(file instanceof ApiClass) return location==StandardLocation.CLASS_PATH;
            return super.contains(location,file);
        }
        public boolean isSameFile(FileObject a,FileObject b) {
            if(a instanceof ApiClass || b instanceof ApiClass) return a.toUri().equals(b.toUri());
            return super.isSameFile(a,b);
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length<3) throw new IllegalArgumentException("API jar, classes directory, source list required");
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null) throw new IllegalStateException("A full JDK is required");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        boolean success;
        try(ZipFile api=new ZipFile(args[0]); StandardJavaFileManager standard=compiler.getStandardFileManager(diagnostics,Locale.ENGLISH,java.nio.charset.StandardCharsets.UTF_8)) {
            ApiManager manager=new ApiManager(standard,api);
            List<String> sources=Files.readAllLines(Paths.get(args[2]),java.nio.charset.StandardCharsets.UTF_8);
            List<String> options=Arrays.asList("--release","8","-Xlint:-options","-encoding","UTF-8","-proc:none","-classpath","","-d",args[1]);
            success=compiler.getTask(new PrintWriter(System.err,true),manager,diagnostics,options,null,standard.getJavaFileObjectsFromStrings(sources)).call();
        }
        List<String> details=new ArrayList<>(); int notes=0; int warnings=0;
        for(Diagnostic<? extends JavaFileObject> d:diagnostics.getDiagnostics()) {
            String line=d.getKind()+": "+d.getSource()+":"+d.getLineNumber()+": "+d.getMessage(Locale.ENGLISH);
            details.add(line);
            if(d.getKind()==Diagnostic.Kind.NOTE) notes++;
            else { System.err.println(line); if(d.getKind()==Diagnostic.Kind.WARNING || d.getKind()==Diagnostic.Kind.MANDATORY_WARNING) warnings++; }
        }
        if(args.length>=4) Files.write(Paths.get(args[3]),details,java.nio.charset.StandardCharsets.UTF_8);
        // Clean builds must not silently accept a compiler warning.
        if(warnings>0) success=false;
        if(!success) System.exit(1);
        System.out.println("Compilation completed: "+Files.readAllLines(Paths.get(args[2])).size()+" source files; errors=0 warnings=0 informationalNotes="+notes);
    }
}