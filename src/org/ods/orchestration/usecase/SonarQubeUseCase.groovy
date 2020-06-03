package org.ods.orchestration.usecase

import org.ods.util.IPipelineSteps
import org.ods.orchestration.util.Project
import org.ods.services.NexusService

@SuppressWarnings(['JavaIoPackageAccess', 'EmptyCatchBlock'])
class SonarQubeUseCase {

    private Project project
    private NexusService nexus
    private IPipelineSteps steps

    SonarQubeUseCase(Project project, IPipelineSteps steps, nexus) {
        this.project = project
        this.steps = steps
        this.nexus = nexus
    }

    List<Map<String, Object>> loadReportsFromPath(String path) {
        def result = []

        this.steps.dir(path) {
            this.steps.findFiles(glob: '**/*.md').each { fileInfo ->
                if(!fileInfo.directory) {
                    result << [name: fileInfo.name, path: fileInfo.path, directory: false,
                               length: fileInfo.length, lastModified: fileInfo.lastModified]
                }
            }
        }

        return result
    }

    String uploadReportToNexus(String version, Map repo, String type, String artifact) {
        def repository = (String) this.project.services.nexus.repository.name
        return this.nexus.storeArtifactFromFile(
            repository,
            "${this.project.key.toLowerCase()}-${version}",
            "${type}-${repo.id}-${version}.md",
            artifact,
            "application/text"
        ).toString()
    }
}
