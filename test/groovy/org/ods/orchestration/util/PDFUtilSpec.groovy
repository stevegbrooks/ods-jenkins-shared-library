package org.ods.orchestration.util

import org.apache.pdfbox.pdmodel.PDDocument
import util.FixtureHelper
import util.PipelineSteps
import util.SpecHelper

class PDFUtilSpec extends SpecHelper {


    def "add watermark text"() {
        given:
        def util = new PDFUtil(new PipelineSteps())

        def pdfFile = new FixtureHelper().getResource("Test-1.pdf")
        def text = "myWatermark"

        when:
        def result = util.addWatermarkText(pdfFile.bytes, text)

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 1
        doc.getPage(0).getContents().text.contains(text)
        doc.close()
    }

    def "convert from mardkdown document"() {
        given:
        def util = new PDFUtil(new PipelineSteps())

        def docFile = new FixtureHelper().getResource("Test.md")
        def result

        when:
        result = util.convertFromMarkdown(docFile.getPath(), false)

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 2
        doc.close()

        when:
        result = util.convertFromMarkdown(docFile.getPath(), true)

        then:
        def docLandscape = PDDocument.load(result)
        docLandscape.getNumberOfPages() == 4
        docLandscape.close()

    }

    def "convert from Microsoft Word document"() {
        given:
        def util = new PDFUtil(new PipelineSteps())

        def docFile = new FixtureHelper().getResource("Test.docx")

        when:
        def result = util.convertFromWordDoc(docFile.getPath())

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 1
        doc.close()
    }

    def "merge documents"() {
        given:
        def util = new PDFUtil(new PipelineSteps())

        def docFile1 = new FixtureHelper().getResource("Test-1.pdf")
        def docFile2 = new FixtureHelper().getResource("Test-2.pdf")

        when:
        def result = util.merge([docFile1.bytes, docFile2.bytes])

        then:
        new String(result).startsWith("%PDF-1.4\n")

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 2
        doc.close()
    }
}
