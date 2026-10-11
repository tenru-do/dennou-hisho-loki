import java.nio.file.*;
import java.util.zip.*;
import java.io.*;

/** Windows aapt2 can embed backslashes in nested asset names. Android requires '/'. */
class NormalizeWebAssets {
    public static void main(String[] args) throws Exception {
        Path apk=Path.of(args[0]).toAbsolutePath(), temp=Files.createTempFile(apk.getParent(),"web-assets-",".apk");
        try {
            try(ZipFile input=new ZipFile(apk.toFile());ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(temp))) {
                var entries=input.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry source=entries.nextElement();String name=source.getName();
                    if(name.startsWith("assets/loki-web"))name=name.replace('\\','/');
                    ZipEntry target=new ZipEntry(name);target.setMethod(source.getMethod());target.setTime(source.getTime());
                    if(source.getMethod()==ZipEntry.STORED){target.setSize(source.getSize());target.setCrc(source.getCrc());}
                    output.putNextEntry(target);if(!source.isDirectory())try(InputStream in=input.getInputStream(source)){in.transferTo(output);}output.closeEntry();
                }
            }
            Files.move(temp,apk,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
}
