<?xml version="1.0" encoding="UTF-8"?>
<!-- produce xml for use with Solr 4.1 -->
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                exclude-result-prefixes="xsl xs">
    
    <xsl:output indent="yes" encoding="UTF-8" />
    <xsl:strip-space elements="*"/>

    <xsl:template match="text()"><xsl:value-of select="normalize-space(.)"/></xsl:template>
        
    <xsl:template match="/list">
        <add>
        <xsl:for-each select="item">
			<doc>
			     <field name="id"><xsl:value-of select="@id" /></field>         
			     <field name="type_s"><xsl:value-of select="@type" /></field>
			     <field name="test_s">TEST</field>          
			</doc>                
        </xsl:for-each>
        </add>
    </xsl:template>
</xsl:stylesheet>