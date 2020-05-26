package org.ods.orchestration.util

@Grab('net.lingala.zip4j:zip4j:2.1.1')
@Grab('org.yaml:snakeyaml:1.24')

import com.cloudbees.groovy.cps.NonCPS

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.apache.commons.io.FilenameUtils
import org.ods.util.IPipelineSteps
import org.ods.services.GitService

@SuppressWarnings('JavaIoPackageAccess')
class PipelineUtil {

    static final String ARTIFACTS_BASE_DIR = 'artifacts'
    static final String SONARQUBE_BASE_DIR = 'sonarqube'
    static final String XUNIT_DOCUMENTS_BASE_DIR = 'xunit'

    protected Project project
    protected IPipelineSteps steps
    protected GitService git

    PipelineUtil(Project project, IPipelineSteps steps, GitService git) {
        this.project = project
        this.steps = steps
        this.git = git
    }

    void archiveArtifact(String path, byte[] data) {
        if (!path?.trim()) {
            throw new IllegalArgumentException(
                "Error: unable to archive artifact. 'path' is undefined."
            )
        }

        def wsPath = this.steps.env.WORKSPACE
        if (!path.startsWith(wsPath)) {
            throw new IllegalArgumentException(
                "Error: unable to archive artifact. 'path' must be inside the Jenkins workspace: ${path}"
            )
        }

        def artifactPath = path.substring(wsPath)
        if(artifactPath.startsWith('/')){
            artifactPath=artifactPath.substring(1)
        }

        def base64 = Base64.getEncoder().encodeToString(data)
        this.steps.writeFile(artifactPath, base64, 'Base64')

        // Archive the artifact (requires a relative path inside the Jenkins workspace)
        this.steps.archiveArtifacts(artifactPath)
    }

    @NonCPS
    protected void createDirectory(String path) {
        if (!path?.trim()) {
            throw new IllegalArgumentException("Error: unable to create directory. 'path' is undefined.")
        }
        this.steps.sh("mkdir ${path}")
    }

    byte[] createZipArtifact(String name, Map<String, byte[]> files, boolean archive = true) {
        if (!name?.trim()) {
            throw new IllegalArgumentException("Error: unable to create Zip artifact. 'name' is undefined.")
        }

        if (files == null) {
            throw new IllegalArgumentException("Error: unable to create Zip artifact. 'files' is undefined.")
        }

        def path = "${this.steps.env.WORKSPACE}/${ARTIFACTS_BASE_DIR}/${name}".toString()
        def result = this.createZipFile(path, files, archive)

        return result
    }

    void createAndStashArtifact(String stashName, byte[] file) {
        if (!stashName?.trim()) {
            throw new IllegalArgumentException("Error: unable to stash artifact. 'name' is undefined.")
        }

        if (file == null) {
            throw new IllegalArgumentException("Error: unable to stash artifact. 'file' is undefined.")
        }

        def path = "${this.steps.env.WORKSPACE}/${ARTIFACTS_BASE_DIR}".toString()

        // Parent directory will be automatically created if needed
        this.steps.dir(path) {
            this.steps.writeFile(stashName, Base64.getEncoder().encodeToString(file), 'Base64')
            this.steps.stash(['name': stashName, 'includes': stashName])
        }
    }

    @NonCPS
    byte[] createZipFile(String path, Map<String, byte[]> files, boolean archive = false) {
        if (!path?.trim()) {
            throw new IllegalArgumentException("Error: unable to create Zip file. 'path' is undefined.")
        }

        if (files == null) {
            throw new IllegalArgumentException("Error: unable to create Zip file. 'files' is undefined.")
        }

        this.steps.dir(FilenameUtils.getPath(path)){
            this.steps.dir('__tmp'){
                files.each{ filePath, fileData ->
                    this.steps.dir(FilenameUtils.getPath(filePath)){
                        this.steps.writeFile(FilenameUtils.getName(filePath), Base64.getEncoder().encodeToString(fileData), 'Base64')
                    }
                }
            }
            def fileName = FilenameUtils.getName(path)
            this.steps.zip(fileName, archive, '__tmp')
            this.steps.dir('__tmp') {
                this.steps.deleteDir()
            }
            def base64 = this.steps.readFile(fileName, 'Base64')
            return Base64.getDecoder().decode(base64)
        }
    }

    @NonCPS
    byte[] extractFromZipFile(String path, String fileToBeExtracted) {
        this.steps.dir(FilenameUtils.getPath(path)) {
            this.steps.unzip(zipFile: FilenameUtils.getName(path), glob: fileToBeExtracted)
            def base64 = this.steps.readFile(fileToBeExtracted)
            return Base64.getDecoder().decode(base64)
        }
    }

    void executeBlockAndFailBuild(Closure block) {
        try {
            block()
        } catch (e) {
            this.failBuild(e.message)
            hudson.Functions.printThrowable(e)
            throw e
        }
    }

    void failBuild(String message) {
        this.steps.currentBuild.result = 'FAILURE'
        this.steps.echo(message)
    }

    void warnBuild(String message) {
        this.steps.currentBuild.result = 'UNSTABLE'
        this.steps.echo(message)
    }

    def loadGroovySourceFile(String path) {
        if (!path?.trim()) {
            throw new IllegalArgumentException("Error: unable to load Groovy source file. 'path' is undefined.")
        }

        if (!this.steps.fileExists(path)) {
            throw new IllegalArgumentException("Error: unable to load Groovy source file. Path ${path} does not exist.")
        }

        return this.steps.load(path)
    }

}
