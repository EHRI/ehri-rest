<?xml version='1.0'?>
<!--
//*****************************************************************************
// Pre-process EAD files from the Wiener Library to join values from <emph> 
// elements within access points.
//
// Distributed under the GNU General Public Licence
//*****************************************************************************
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
    <xsl:output encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="archdesc">
        <archdesc>
            <xsl:attribute name="level">
                <xsl:value-of select="@level"/>
            </xsl:attribute>
            <xsl:attribute name="otherlevel">
                <xsl:value-of select="@otherlevel"/>
            </xsl:attribute>
            <xsl:attribute name="id">
                <xsl:value-of select="/ead/eadheader/eadid"/>
            </xsl:attribute>

            <xsl:apply-templates select="child::node()"/>
        </archdesc>
    </xsl:template>

    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
