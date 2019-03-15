<?xml version='1.0'?>
<!--
//*****************************************************************************
// Pre-process EAD files to add a langusage to the eadheader
//
// Distributed under the GNU General Public Licence
//*****************************************************************************
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xlink="https://www.w3.org/1999/xlink">
    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
    <xsl:output encoding="UTF-8"/>
<!--    <xsl:param name="langcode" as="xs:string" select="'fre'"/>-->
    <xsl:param name="langcode" as="xs:string" required="yes"/>
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="profiledesc">
        <profiledesc>
            <xsl:copy-of select="creation"/>
            <langusage>
                <language>
                    <xsl:attribute name="langcode">
                        <xsl:value-of select="$langcode"/>
                    </xsl:attribute>
                    <xsl:value-of select="$langcode"/>
                </language>
            </langusage>
            <xsl:copy-of select="descrules"/>
        </profiledesc>
    </xsl:template>

    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
