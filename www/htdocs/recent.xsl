<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns="http://www.w3.org/1999/xhtml">

<xsl:output method="html"/>

<xsl:template match="atom:feed">
    <html>
    <head>
        <meta name="GENERATOR" content="recent.xsl"/>
        <link rel="StyleSheet" type="text/css" href="site/style.css"/>
        <link rel="alternate" type="application/atom+xml" title="Atom (recent update summaries)" href="recent.xml"/>
        <link rel="icon" type="image/gif" href="{atom:icon}"/>
        <title><xsl:value-of select="atom:title"/>: <xsl:value-of select="atom:subtitle"/></title>
    </head>
    <body>
        <div class="navigation heading">
        <a href="./"><img alt="home" src="{atom:logo}" border="0"/></a>
        &#x2192; <a class="heading" href=""><xsl:value-of select="atom:title"/></a>
        </div>
        <div class="main">
        <h1><a name="subtitle" href="#subtitle"><xsl:value-of select="atom:subtitle"/></a></h1>
        <xsl:for-each select="atom:entry">
            <div class="entry">
            <span class="dateTime comment"><xsl:value-of select="atom:updated"/></span>
            <xsl:text> </xsl:text>
            <a class="heading" href="{atom:link/@href}"><xsl:value-of select="atom:title"/></a>
            <xsl:text> </xsl:text>
            <xsl:copy-of select="atom:content/*"/>
            </div>
        </xsl:for-each>
        </div>
        <p class="footer comment">
        <a class="author" href="{atom:author/atom:uri}"><xsl:value-of select="atom:author/atom:name"/></a>,
        <span class="dateTime comment"><xsl:value-of select="atom:updated"/></span>
        </p>
    </body>
    </html>
</xsl:template>

</xsl:stylesheet>
