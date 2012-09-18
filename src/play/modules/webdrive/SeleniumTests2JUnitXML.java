package play.modules.webdrive;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import play.Logger;
import play.libs.IO;

import com.google.common.collect.Lists;

public class SeleniumTests2JUnitXML {
  private static final String REGEX_HTML_META_TAG_CLOSED = "<meta $1 />";
  private static final String REGEX_HTML_META_TAG_INVALID = "<meta(.*)>";
  private static final String REGEX_HTML_ENTITY_WHITESPACE = "&nbsp;";
  private static final String REGEX_HTML_WHITESPACE = " ";

  private static final String XML_EXT = ".xml";
  private static final String HTML_EXT = ".html";
  static final String XALAN_INDENT_AMOUNT = "{http://xml.apache.org/xslt}" + "indent-amount";

  public static void main(String[] args) {
    Logger.info(" ~ selenium tests will be processed for analysis by bamboo");
    List<String> paths = Lists.newArrayList();
    paths.add("test-result/FirefoxDriver/");
    paths.add("test-result/ChromeDriver/");
    File xslFile = new File("modules/webdrive-0.2.custom/src/resource/htmlResult2JUnitResult.xsl");
    Logger.debug(" ~ XSL file for transformation used is '%s'", xslFile.getAbsolutePath());
    Logger.debug("   translation: plays selenium test (html result page) --> junit xml file (bamboo readable)");
    for (String path : paths) {
      Logger.debug("process " + path);
      File seleniumTestFolder = new File(path);
      if (seleniumTestFolder.exists() || seleniumTestFolder.isDirectory()) {
        List<File> seleniumTests = Arrays.asList(seleniumTestFolder.listFiles());
        for (File seleniumTest : seleniumTests) {
          try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslFile));
            transformer.setOutputProperty(XALAN_INDENT_AMOUNT, "4");
            if (seleniumTest != null) {
              String absolutePathSeleniumTest = seleniumTest.getAbsolutePath();
              if (absolutePathSeleniumTest.endsWith(HTML_EXT)) {

                File seleniumTestFile = new File(absolutePathSeleniumTest);
                String htmlResultContent = IO.readContentAsString(seleniumTestFile);
                htmlResultContent = htmlResultContent.replaceAll(REGEX_HTML_META_TAG_INVALID, REGEX_HTML_META_TAG_CLOSED);
                htmlResultContent = htmlResultContent.replaceAll(REGEX_HTML_ENTITY_WHITESPACE, REGEX_HTML_WHITESPACE);
                IO.writeContent(htmlResultContent, seleniumTestFile);
                File resultFile = new File(seleniumTestFile + XML_EXT);
                FileOutputStream resultOutputStream = new FileOutputStream(resultFile);
                StreamSource streamSource = new StreamSource(seleniumTest);
                StreamResult streamResultAfterXSLProcessing = new StreamResult(resultOutputStream);
                transformer.transform(streamSource, streamResultAfterXSLProcessing);
              }
            }
          } catch (Throwable t) {
            Logger.error(t, t.getMessage());
          }
        }
      } else {
        Logger.error("path '%s' is not ok: exists: '%s', is directory: '%s'", path, Boolean.toString(seleniumTestFolder.exists()),
            Boolean.toString(seleniumTestFolder.isDirectory()));
      }
    }
  }
}
