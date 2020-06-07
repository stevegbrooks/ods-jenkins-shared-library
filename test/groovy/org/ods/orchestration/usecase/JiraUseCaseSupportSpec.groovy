package org.ods.orchestration.usecase

import org.ods.orchestration.service.*
import org.ods.orchestration.util.*
import org.ods.services.ServiceRegistry
import org.ods.util.IPipelineSteps

import java.nio.file.Files
import java.nio.file.Paths

import static util.FixtureHelper.*

import util.*

class JiraUseCaseSupportSpec extends SpecHelper {

    JiraUseCase createUseCase(IPipelineSteps steps, MROPipelineUtil util, JiraService jira) {
        return new JiraUseCase(steps, util, jira)
    }

    JiraUseCaseSupport createUseCaseSupport(IPipelineSteps steps, JiraUseCase usecase) {
        return new JiraUseCaseSupport(steps, usecase)
    }

    Project project
    IPipelineSteps steps
    JiraUseCase usecase
    JiraUseCaseSupport support

    def setup() {
        project = createProject()
        def steps = Spy(FakePipelineSteps)
        def tmpDir = getClass().getSimpleName()
        def tmpPath = Paths.get(steps.env.WORKSPACE, tmpDir)
        Files.createDirectories(tmpPath)
        steps.env.WORKSPACE = tmpPath.toString()
        this.steps = steps
        ServiceRegistry.instance.add(IPipelineSteps, steps)
        usecase = Mock(JiraUseCase)

        support = new JiraUseCaseSupport(project, steps, usecase)
        usecase.setSupport(support)
    }

    def "apply test results to test issues"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResults()

        when:
        support.applyXunitTestResults(testIssues, testResults)

        then:
        1 * usecase.applyXunitTestResultsAsTestIssueLabels(testIssues, testResults)
    }
}
