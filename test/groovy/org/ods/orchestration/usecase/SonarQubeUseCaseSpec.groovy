package org.ods.orchestration.usecase

import org.ods.services.ServiceRegistry

import java.nio.file.Files

import org.ods.orchestration.util.*
import org.ods.services.NexusService
import org.ods.util.IPipelineSteps

import java.nio.file.Paths

import static util.FixtureHelper.*

import util.*

class SonarQubeUseCaseSpec extends SpecHelper {

    NexusService nexus
    Project project
    IPipelineSteps steps
    SonarQubeUseCase usecase

    def setup() {
        project = createProject()
        def steps = Spy(FakePipelineSteps)
        def tmpDir = getClass().getSimpleName()
        def tmpPath = Paths.get(steps.env.WORKSPACE, tmpDir)
        Files.createDirectories(tmpPath)
        steps.env.WORKSPACE = tmpPath.toString()
        this.steps = steps
        ServiceRegistry.instance.add(IPipelineSteps, steps)
        nexus = Mock(NexusService)
        usecase = new SonarQubeUseCase(project, steps, nexus)
    }

    def "load reports from path"() {
        given:
        def sqFiles = Files.createTempDirectory("sq-reports-")
        def sqFile1 = Files.createTempFile(sqFiles, "sq", ".md") << "SQ Report 1"
        def sqFile2 = Files.createTempFile(sqFiles, "sq", ".md") << "SQ Report 2"

        when:
        def result = usecase.loadReportsFromPath(sqFiles.toString())

        then:
        result.size() == 2
        result.collect { new File(it.path).text }.sort() == ["SQ Report 1", "SQ Report 2"]

        cleanup:
        sqFiles.toFile().deleteDir()
    }

    def "load SQ reports from path with empty path"() {
        given:
        def sqFiles = Files.createTempDirectory("sq-reports-")

        when:
        def result = usecase.loadReportsFromPath(sqFiles.toString())

        then:
        result.isEmpty()

        cleanup:
        sqFiles.toFile().deleteDir()
    }

    def "upload SQ reports to Nexus"() {
        given:
        def version = "0.1"
        def repo = project.repositories.first()
        def type = "myType"
        def artifact = Files.createTempFile("sq", ".md").toFile()

        when:
        def result = usecase.uploadReportToNexus(version, repo, type, artifact.toString())

        then:
        1 * nexus.storeArtifactFromFile(
            project.services.nexus.repository.name,
            { "${project.key.toLowerCase()}-${version}" },
            { "${type}-${repo.id}-${version}.md" },
            artifact.toString(),
            "application/text"
        )

        cleanup:
        artifact.delete()
    }
}
