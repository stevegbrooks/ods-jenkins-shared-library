package util


import org.apache.commons.lang3.SystemUtils
import org.ods.util.IPipelineSteps

import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@SuppressWarnings('MethodCount')
class PipelineSteps implements IPipelineSteps {

    private Map currentBuild = [:]
    private Map env = [:]
    private ThreadLocal<Path> dirContext = new ThreadLocal<>()

    PipelineSteps() {
        env.WORKSPACE = System.getProperty("java.io.tmpdir")
    }

    private Path getBasePath() {
        return dirContext.get() ?: Paths.get(env.WORKSPACE)
    }

    private Path getPath(String path) {
        return resolve(basePath, path)
    }

    private Path resolve(Path basePath, String path) {
        if (isUnix()) {
            path.replace('\\' as char, '/' as char)
        } else {
            path.replace('/' as char, '\\' as char)
        }
        return basePath.resolve(path)
    }

    private void processException(AutoCloseable c, Throwable t) {
        try {
            c.close()
        } catch (Throwable suppress) {
            t.addSuppressed(suppress)
        }
        throw t
    }

    void archiveArtifacts(String artifacts) {
    }

    def checkout(Map config) {
        return [:]
    }

    def dir(String path, Closure block) {
        if (path.isAllWhitespace()) {
            return block()
        }
        def ret
        def basePath = getBasePath()
        def newPath = resolve(basePath, path)
        Files.createDirectories(newPath)
        dirContext.set(newPath)
        try {
            ret = block()
        } finally {
            dirContext.set(basePath)
        }
        return ret
    }

    void echo(String message) {
    }

    def getCurrentBuild() {
        return currentBuild
    }

    Map getEnv() {
        return env
    }

    void junit(String path) {
    }

    void junit(Map config) {
    }

    def load(String path) {
        return [:]
    }

    def sh(def args) {
        return Runtime.getRuntime().exec("sh -c ${args.script}")
    }

    @Override
    def bat(def args) {
        return Runtime.getRuntime().exec("cmd /c ${args.script}")
    }

    void stage(String name, Closure block) {
        block()
    }

    @Override
    void stash(String name) {
    }

    void stash(Map config) {
    }

    void unstash(String name) {
    }

    @Override
    def fileExists(String file) {
        def path = getPath(file)
        return Files.exists(path)
    }

    @Override
    def readFile(String file, String encoding = null) {
        def path = getPath(file)
        if (encoding == 'Base64') {
            def base64 = path.bytes.encodeBase64().toString()
            return base64
        }
        def text = encoding ? path.getText(encoding) : path.text
        return text
    }

    @Override
    def readFile(Map args) {
        return readFile(args.file, args.encoding)
    }

    @Override
    def writeFile(String file, String text, String encoding = null) {
        def path = getPath(file)
        if (encoding == 'Base64') {
            def bytes = text.decodeBase64()
            path.bytes = bytes
        } else if (encoding) {
            path.setText(text, encoding)
        } else {
            path.text = text
        }
    }

    @Override
    def writeFile(Map args) {
        return writeFile(args.file, args.text, args.encoding)
    }

    @Override
    def readJSON(Map args) {
        return null
    }

    @Override
    def writeJSON(Map args) {
        return null
    }

    @Override
    def timeout(Map args, Closure block) {
        return null
    }

    @Override
    def deleteDir() {
        def basePath = getBasePath()
        Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc) {
                    throw exc
                }
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    def sleep(int seconds) {
        return null
    }

    @Override
    def withEnv(java.util.List env, groovy.lang.Closure block) {
      block()
    }

    @Override
    def unstable(String message) {
    }

    def usernamePassword(Map credentialsData) {
    }

    def sshUserPrivateKey(Map credentialsData) {
    }

    def withCredentials(List credentialsList, Closure block) {
      block()
    }

    def get(def key) {
      return currentBuild.get(key)
    }

    def put(def key, def value) {
      currentBuild.put(key, value)
    }

    def unwrap() {
      return [:]
    }

    @Override
    def zip(Map args) {
        if(!args.dir) {
            args.dir = ''
        }
        if(!args.glob) {
            args.glob = ''
        }
        def basePath = getBasePath()
        def zipPath = resolve(basePath, args.zipFile)
        def dirPath = resolve(basePath, args.dir)
        zipPath.withOutputStream { os ->
            new ZipOutputStream(new BufferedOutputStream(os)).withStream { zos ->
                def stream = deepFindFiles(dirPath, args.glob)
                try{
                    stream.filter({ file -> Files.isRegularFile(file) }).forEach() {
                        file ->
                            try {
                                def attrs = Files.readAttributes(file, BasicFileAttributes.class)
                                def entry = new ZipEntry(file.toString())
                                entry.creationTime = attrs.creationTime()
                                entry.lastModifiedTime = attrs.lastModifiedTime()
                                entry.lastAccessTime = attrs.lastAccessTime()
                                entry.size = attrs.size()
                                def cos = new CountingSinkOutputStream()
                                def dos = new DeflaterOutputStream(cos)
                                def crcOs = new CRC32OutputStream(dos)
                                Files.copy(file, crcOs)
                                crcOs.close()
                                entry.crc = crcOs.crc
                                if (cos.count < entry.size) {
                                    entry.method = ZipEntry.DEFLATED
                                    entry.compressedSize = cos.count
                                } else {
                                    entry.method = ZipEntry.STORED
                                    entry.compressedSize = entry.size
                                }
                                zos.putNextEntry(entry)
                                Files.copy(file, zos)
                                zos.closeEntry()
                            } catch (IOException e) {
                                throw new RuntimeException(e)
                            }
                    }
                } catch(Exception e) {
                    processException(stream, e)
                }
                stream.close()
            }
        }
        if (args.archive) {
            archiveArtifacts(zipPath.toString())
        }
    }

    @Override
    def unzip(Map args) {
        if(!args.dir) {
            args.dir = ''
        }
        if(!args.glob) {
            args.glob = ''
        }
        def basePath = getBasePath()
        def zipPath = resolve(basePath, args.zipFile)
        def dirPath = resolve(basePath, args.dir)
        def zf = args.charset ? new ZipFile(zipPath.toFile(), Charset.forName(args.charset)) :
            new ZipFile(zipPath.toFile())
        zf.withCloseable {
            zf.stream().forEach() { entry ->
                def entryName = entry.getName()
                //TODO Implement glob
                def entryPath = resolve(dirPath, entryName)
                zf.getInputStream(entry).withStream { is ->
                    def bis = new BufferedInputStream(is)
                    Files.copy(bis, entryPath)
                }
            }
        }
    }

    @Override
    def findFiles(Map args) {
        def result = []
        def basePath = getBasePath()
        Stream<Path> stream = (args.glob ? deepFindFiles(basePath, args.glob) : shallowFindFiles(basePath))
        try {
            if (args.excludes) {
                def matcher = FileSystems.getDefault().getPathMatcher("glob:${args.excludes}");
                stream = stream.filter() {
                    file -> !matcher.matches(file)
                }
            }
            stream.map({ file -> wrapFile(file) })
                .forEach() {
                    file -> result << file
                }
        } catch (Throwable t) {
            processException(stream, t)
        }
        stream.close()
        return result
    }

    private def wrapFile(Path file) {
        def attrs = Files.readAttributes(file, BasicFileAttributes.class)
        return new FileInfo(file.fileName.toString(),
            file.toString(),
            attrs.directory,
            attrs.size(),
            attrs.lastModifiedTime().toMillis())
    }

    private Stream<Path> deepFindFiles(Path basePath, String glob = null) {
        Stream<Path> stream = Files.walk(basePath)
        try {
            if (glob) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:${glob}");
                stream = stream.filter() {
                    file -> matcher.matches(file)
                }
            }
            return stream
        } catch (Throwable t) {
            processException(stream, t)
        }
    }

    private Stream<Path> shallowFindFiles(Path basePath) {
        return Files.list(basePath)
    }

    @Override
    def isUnix() {
        return SystemUtils.IS_OS_UNIX
    }
}
