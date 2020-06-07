package org.ods.services

import org.ods.util.IPipelineSteps
import util.*
import org.ods.util.Logger

import java.nio.file.Files
import java.nio.file.Paths

class JenkinsServiceSpec extends SpecHelper {

    IPipelineSteps steps

    def setup() {
        def steps = Spy(FakePipelineSteps)
        def tmpDir = getClass().getSimpleName()
        def tmpPath = Paths.get(steps.env.WORKSPACE, tmpDir)
        Files.createDirectories(tmpPath)
        steps.env.WORKSPACE = tmpPath.toString()
        this.steps = steps
        ServiceRegistry.instance.add(IPipelineSteps, steps)
    }

    def "unstash files into path"() {
        given:
        def service = new JenkinsService(steps, new Logger(steps, false))

        def name = "myStash"
        def path = "myPath"
        def type = "myType"

        when:
        def result = service.unstashFilesIntoPath(name, path, type)

        then:
        1 * steps.dir(path, _)

        then:
        1 * steps.unstash(name)

        then:
        result == true
    }

    def "unstash files into path with failure"() {
        given:
        def service = new JenkinsService(steps, new Logger(steps, false))

        def name = "myStash"
        def path = "myPath"
        def type = "myType"

        when:
        def result = service.unstashFilesIntoPath(name, path, type)

        then:
        1 * steps.unstash(name) >> {
            throw new RuntimeException()
        }

        then:
        1 * steps.echo("Could not find any files of type '${type}' to unstash for name '${name}'")

        then:
        result == false
    }
}
