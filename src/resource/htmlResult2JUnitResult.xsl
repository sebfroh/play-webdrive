<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" indent="yes"/>
<xsl:template match="/">
<xsl:variable name="countTestsTotal" select="count(//thead)"/>
<xsl:variable name="countTestsFailed" select="count(//tr[@class='  status_failed'])" />
<xsl:variable name="testGroupName" select="normalize-space(string(//h1/text()))" />
<testsuite failures="0" time="0.137" errors="{$countTestsFailed}" skipped="0" tests="{$countTestsTotal}" name="{$testGroupName}">

<xsl:for-each select="//table">
<xsl:variable name="testName" select="string(./thead/tr/th/text())" />
<xsl:choose>
<xsl:when test=".//tr[@class='title status_failed']">
  <xsl:variable name="errorCause" select="string(.)"></xsl:variable>
  <xsl:variable name="failedFunction" select=".//tr[@class='  status_failed']/td[0]"></xsl:variable>
  <xsl:variable name="failedValue" select=".//tr[@class='  status_failed']/td[1]"></xsl:variable>
  <xsl:variable name="failedResultValue" select=".//tr[@class='  status_failed']/td[2]"></xsl:variable>
  <testcase classname="{$testGroupName}"  name="{$testName}" time="0.0">
        <failure type="" message="In {$testGroupName}
          {$failedFunction}({$failedValue}) result: {$failedResultValue}
          Error cause: {$errorCause}">
        </failure>
  </testcase>
</xsl:when>
<xsl:otherwise>
  <testcase classname="{$testGroupName}"  name="{$testName}" time="0.0"></testcase>
</xsl:otherwise>
</xsl:choose>
</xsl:for-each>

</testsuite>

</xsl:template>

</xsl:stylesheet> 