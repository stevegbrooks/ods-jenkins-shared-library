package org.ods.orchestration.service

@Grab('org.yaml:snakeyaml:1.24')
import org.ods.util.IPipelineSteps
import org.yaml.snakeyaml.Yaml

class LeVADocumentChaptersFileService {

    static final String DOCUMENT_CHAPTERS_BASE_DIR = "docs"

    private IPipelineSteps steps

    LeVADocumentChaptersFileService(IPipelineSteps steps) {
        this.steps = steps
    }

    Map getDocumentChapterData(String documentType) {
        if (!documentType?.trim()) {
            throw new IllegalArgumentException(
                "Error: unable to load document chapters. 'documentType' is undefined."
            )
        }

        def String yamlText
        def fileName = "${documentType}.yaml"
        this.steps.dir(this.steps.env.WORKSPACE) {
            this.steps.dir(DOCUMENT_CHAPTERS_BASE_DIR) {
                if (this.steps.fileExists(fileName)) {
                    yamlText = this.steps.readFile(fileName)
                } else {
                    this.steps.dir(docs) {
                        yamlText = this.steps.readFile(fileName)
                    }
                }
            }
        }

        def data = [:]
        if (!yamlText) {
            throw new RuntimeException(
                "Error: unable to load document chapters. File 'docs/${documentType}.yaml' could not be read."
            )
        } else {
            data = new Yaml().load(yamlText) ?: [:]
        }

        return data.collectEntries { chapter ->
            def number = chapter.number.toString()
            chapter.number = number
            chapter.content = chapter.content ?: ""
            chapter["status"] = "done"
            [ "sec${number.replaceAll(/\./, "s")}".toString(), chapter ]
        }
    }
}
