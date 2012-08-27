package play.modules.webdrive;

import java.io.File;
import java.io.FileFilter;

public class QUnitXMLFileFilter implements FileFilter {

  private static String QUNIT_TEST_RESULT = "qunit.";
  private static String XML_FILETYPE = ".xml";

  @Override
  public boolean accept(File file) {
    return file.canRead() && !file.isHidden() && file.isFile() && file.getName().toUpperCase().startsWith(QUNIT_TEST_RESULT)
        && file.getName().toUpperCase().endsWith(XML_FILETYPE);
  }
}
