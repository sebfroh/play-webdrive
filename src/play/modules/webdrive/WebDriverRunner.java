/*
 * WebDrive - Selenium 2 WebDriver support for play framework
 * 
 * Copyright (C) 2011 Raghu Kaippully
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package play.modules.webdrive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import play.Logger;

import com.beust.jcommander.internal.Lists;
import com.sun.jna.platform.FileUtils;

public class WebDriverRunner {

  /**
   * Default URL of the play app.
   */
  private static final String DEFAULT_APP_URL = "http://localhost:9000";

  /**
   * Default timeout value for each test.
   */
  private static final String DEFAULT_TEST_TIMEOUT = "120";

  private static final String ENABLE_SELENIUM_KEY = "selenium.enabled";

  private static final String DEFAULT_ENABLE_SELENIUM_TESTS = "TRUE";

  private static final String DEFAULT_ENABLE_JUNIT_TESTS_ONLY = "FALSE";

  private static final String ENABLE_JUNIT_TESTS_ONLY = "junit.enabled.only";

  public static void main(String[] args) throws Exception {
    if (new WebDriverRunner().run())
      System.exit(0);
    else
      System.exit(1);
  }

  /**
   * Set to false if selenium tests must not run.
   */
  private boolean _enableSeleniumTest;

  /**
   * Set to true if only junit tests should run
   */
  private boolean _enableJUnitTestsOnly;

  /**
   * The "test-result" directory.
   */
  private File testResultRoot;

  /**
   * Set to true if any of the tests failed.
   */
  private boolean failed;

  /**
   * URL of the play application to test
   */
  private String appUrlBase;

  /**
   * The maximum time we give for each test to complete.
   */
  private int testTimeoutInSeconds;

  /**
   * URL part for selenium test runner
   */
  private String seleniumUrlPart;

  /**
   * All selenium tests.
   */
  private List<String> seleniumTests = new ArrayList<String>();

  /**
   * All non-selenium tests
   */
  private List<String> nonSeleniumTests = new ArrayList<String>();

  private List<String> _junitTests = Lists.newArrayList();
  private int maxTestNameLength;

  public WebDriverRunner() {
    String seleniumStateParameter = System.getProperty(ENABLE_SELENIUM_KEY, DEFAULT_ENABLE_SELENIUM_TESTS);
    _enableSeleniumTest = Boolean.parseBoolean(seleniumStateParameter);
    String enableJUnitOnly = System.getProperty(ENABLE_JUNIT_TESTS_ONLY, DEFAULT_ENABLE_JUNIT_TESTS_ONLY);
    _enableJUnitTestsOnly = Boolean.parseBoolean(enableJUnitOnly);
    this.appUrlBase = System.getProperty("application.url", DEFAULT_APP_URL);
    String timeoutStr = System.getProperty("webdrive.timeout", DEFAULT_TEST_TIMEOUT);
    try {
      if (timeoutStr == null || timeoutStr.trim().equals(""))
        timeoutStr = DEFAULT_TEST_TIMEOUT;
      this.testTimeoutInSeconds = Integer.parseInt(timeoutStr);
      Logger.info("~ Using a timeout value of " + this.testTimeoutInSeconds + " seconds");
    } catch (NumberFormatException e) {
      Logger.info("~ The timeout value " + timeoutStr + " is not a " + "number. Setting to default value " + DEFAULT_TEST_TIMEOUT + " seconds");
      this.testTimeoutInSeconds = Integer.parseInt(DEFAULT_TEST_TIMEOUT);
    }
    retrieveTestsList();
    if (_enableJUnitTestsOnly) {
      _junitTests = calculateJUnitTests();
    }
  }

  /**
   * Retrieve the list of tests to run
   */
  private void retrieveTestsList() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new URL(appUrlBase + "/@tests.list").openStream(), "utf-8"));
      String marker = in.readLine();
      if (!marker.equals("---"))
        throw new RuntimeException("Error retrieving list of tests");

      this.testResultRoot = new File(in.readLine());
      this.seleniumUrlPart = in.readLine();
      String line;
      while ((line = in.readLine()) != null) {
        String test = line;
        String testName = test.replace(".class", "").replace(".test.html", "").replace(".", "/").replace("$", "/");
        if (testName.length() > maxTestNameLength) {
          maxTestNameLength = testName.length();
        }
        if (test.contains(".test.html")) {
          if (_enableSeleniumTest) {
            seleniumTests.add(test);
          }
        } else {
          nonSeleniumTests.add(test);
        }
      }

      in.close();
      if (_enableSeleniumTest) {
        Logger.info("~ " + seleniumTests.size() + " selenium test" + (seleniumTests.size() != 1 ? "s" : "") + " to run:");
      } else {
        Logger.info("~ Selenium tests are deactivated.");
      }
      Logger.info("~ " + nonSeleniumTests.size() + " other test" + (nonSeleniumTests.size() != 1 ? "s" : "") + " to run:");
      Logger.info("~");
    } catch (Exception e) {
      Logger.info("~ The application does not start. There are errors: " + e);
      System.exit(-1);
    }
  }

  private List<String> calculateJUnitTests() {
    List<String> allUnitTestNames = Lists.newArrayList();
    String unittestClassname = "play.test.UnitTest";
    for (String classFilename : nonSeleniumTests) {
      try {
        String javaFilenameToParse = classFilename.replace(".", File.separator);
        javaFilenameToParse = javaFilenameToParse.replace(File.separatorChar + "class", ".java");
        File classFile = new File("test", javaFilenameToParse);
        String content = FileUtils.readFileToString(classFile);
        if (content.contains(unittestClassname)) {
          allUnitTestNames.add(classFilename);
        }
      } catch (IOException e) {
        Logger.error(e, e.getMessage());
      }
    }
    return allUnitTestNames;
  }

  private boolean run() throws Exception {
    DriverManager manager = new DriverManager();
    List<Class<?>> driverClasses = manager.getDriverClasses();

    /* Run non-selenium tests */
    if (_enableJUnitTestsOnly) {
      Logger.info("  start to run unit tests: ");
      runTestsWithDriver(HtmlUnitDriver.class, _junitTests);
    } else {
      runTestsWithDriver(HtmlUnitDriver.class, nonSeleniumTests);

      if (_enableSeleniumTest) {
        /* Run selenium tests on all browsers */
        for (Class<?> driverClass : driverClasses) {
          runTestsWithDriver(driverClass, seleniumTests);
        }
      }
    }

    File resultFile = new File(testResultRoot, "result." + (failed ? "failed" : "passed"));
    resultFile.createNewFile();

    return !failed;
  }

  private void runTestsWithDriver(Class<?> webDriverClass, List<String> tests) throws Exception {
    Logger.info("~ Starting tests with " + webDriverClass);
    long startTime = System.currentTimeMillis();
    WebDriver webDriver = (WebDriver) webDriverClass.newInstance();
    webDriver.get(appUrlBase + "/@tests/init");
    boolean ok = true;
    for (String test : tests) {
      long start = System.currentTimeMillis();
      String testName = test.replace(".class", "").replace(".test.html", "").replace(".", "/").replace("$", "/");
      StringBuilder loggerOutput = new StringBuilder("~ " + testName + "... ");

      for (int i = 0; i < maxTestNameLength - testName.length(); i++) {
        loggerOutput.append(" ");
      }
      loggerOutput.append("    ");
      String url;
      if (test.endsWith(".class")) {
        url = appUrlBase + "/@tests/" + test;
      } else {
        url = appUrlBase + seleniumUrlPart + "?baseUrl=" + appUrlBase + "&test=/@tests/" + test + ".suite&auto=true&resultsUrl=/@tests/" + test;
      }
      webDriver.get(url);
      int retry = 0;
      while (retry < testTimeoutInSeconds) {
        if (new File(testResultRoot, test.replace("/", ".") + ".passed.html").exists()) {
          loggerOutput.append("PASSED      ");
          break;
        } else if (new File(testResultRoot, test.replace("/", ".") + ".failed.html").exists()) {
          loggerOutput.append("FAILED   !  ");
          ok = false;
          break;
        } else {
          retry++;
          if (retry == testTimeoutInSeconds) {
            loggerOutput.append("TIMEOUT  ?  ");
            ok = false;
            break;
          }
          Thread.sleep(1000);
        }
      }

      //
      int duration = (int) (System.currentTimeMillis() - start);
      int seconds = (duration / 1000) % 60;
      int minutes = (duration / (1000 * 60)) % 60;

      if (minutes > 0) {
        loggerOutput.append(minutes + " min " + seconds + "s");
      } else {
        loggerOutput.append(seconds + "s");
      }
      Logger.info(loggerOutput.toString());
    }
    webDriver.get(appUrlBase + "/@tests/end?result=" + (ok ? "passed" : "failed"));
    webDriver.quit();

    saveTestResults(webDriver.getClass().getSimpleName());
    if (!ok) {
      failed = true;
    }
    long endTime = System.currentTimeMillis();
    Duration testDuration = new Duration(startTime, endTime);
    String time = testDuration.getStandardSeconds() + " sec.";
    Logger.info(" ~ duration: " + webDriver.getClass().getSimpleName() + " " + time);
  }

  /**
   * Play stores test results under {app.path}/test-result directory. We will
   * move it under {app.path}/test-result/{webDriverName}.
   */
  private void saveTestResults(String webDriverName) {
    File destDir = new File(testResultRoot, webDriverName);
    destDir.mkdir();

    for (File file : testResultRoot.listFiles()) {
      String fileName = file.getName();
      if (!"application.log".equals(fileName)) {
        moveTestResultsIntoOtherFolder(destDir, file);
      }
    }
  }

  public void moveTestResultsIntoOtherFolder(File destDir, File file) {
    if (file.isFile()) {
      File newFile = new File(destDir, file.getName());
      if (!file.renameTo(newFile)) {
        Logger.info("~ Could not create " + newFile);
      }
    }
  }
}
