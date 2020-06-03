package util

class FileInfo {

    private String name
    private String path
    private boolean directory
    private long length
    private long lastModified

    FileInfo(String name, String path, boolean directory, long length, long lastModified) {
        this.name = name
        this.path = path
        this.directory = directory
        this.length = length
        this.lastModified = lastModified
    }

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getPath() {
        return path
    }

    void setPath(String path) {
        this.path = path
    }

    boolean getDirectory() {
        return directory
    }

    void setDirectory(boolean directory) {
        this.directory = directory
    }

    long getLength() {
        return length
    }

    void setLength(long length) {
        this.length = length
    }

    long getLastModified() {
        return lastModified
    }

    void setLastModified(long lastModified) {
        this.lastModified = lastModified
    }
}
