<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      exclude-result-prefixes="xs"
      version="2.0">
    <xsl:output omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <!-- match alles, waarvan het child essentially leeg is, en doe er niets mee -->
    <xsl:template match="*[normalize-space()='']" />
    
    <!-- match alle nodes of attributes -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <!-- kopieer de node of attribute, en ga met het directe child de templates
            weer af, dus recursief deze als het weer een node of attribuut betreft, 
            of de andere als 'ie essentially leeg is
            -->
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>