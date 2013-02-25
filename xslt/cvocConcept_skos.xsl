<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      exclude-result-prefixes="xs"
      version="2.0">
    <xsl:output indent="yes" encoding="UTF-8" />
    <xsl:strip-space elements="*"/>
    <xsl:template match="text()"><xsl:value-of select="normalize-space(.)"/></xsl:template>
    
    <!-- Use keys to retrieve all concepts that have a specific other concept as broader, 
    these are that specific concepts narrower concepts -->
    <!-- Also note that everything with a broader concept will now be in memory -->
    <xsl:key name="concepts-by-broaderid" match="/list/item[@type='cvocConcept']" use="relationships/broader/item[@type='cvocConcept']/@id" />
    
    <!-- Note: use the 'identifier' property instead of the 'id' attribute, 
    because the original URI's have been stored in the identifier on importing from skos. -->
    
    <xsl:template match="/">
        <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:skos="http://www.w3.org/2004/02/skos/core#">

            <xsl:for-each select="list/item[@type='cvocConcept']">
                <!-- <div>concept id: <xsl:value-of select="@id" /></div> -->
                <xsl:variable name="cUri" select="data/property[@name='identifier']"/>
                <rdf:Description rdf:about="{$cUri}">
                    <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
                    <skos:inScheme rdf:resource="{relationships/inCvoc/item/@id}"/>

                    <!-- descriptions (one per language) -->
                    <xsl:for-each
                        select="relationships/describes/item[@type='cvocConceptDescription']">
                        <xsl:variable name="lang" select="data/property[@name='languageCode']"/>
                        <skos:prefLabel xml:lang="{$lang}"><xsl:value-of select="data/property[@name='prefLabel']"/></skos:prefLabel>
                        <xsl:for-each select="data/propertySequence/property[@name='altLabel']">
                            <skos:altLabel xml:lang="{$lang}"><xsl:value-of select="text()"/></skos:altLabel>
                        </xsl:for-each>
                        <xsl:for-each select="data/propertySequence/property[@name='scopeNote']">
                            <skos:scopeNote xml:lang="{$lang}"><xsl:value-of select="text()"/></skos:scopeNote>
                        </xsl:for-each>
                        <xsl:for-each select="data/propertySequence/property[@name='definition']">
                            <skos:definition xml:lang="{$lang}"><xsl:value-of select="text()"/></skos:definition>
                        </xsl:for-each>
                    </xsl:for-each>
                    
                    <!-- broader concepts -->
                    <xsl:for-each select="relationships/broader/item[@type='cvocConcept']">
                        <skos:broader rdf:resource="{data/property[@name='identifier']}"/>
                    </xsl:for-each>

                    <!-- narrower concepts -->
                    <xsl:for-each select="key('concepts-by-broaderid', @id)">
                        <skos:narrower rdf:resource="{data/property[@name='identifier']}"/>
                    </xsl:for-each>

                    <!-- related (and relatedBy) is MISSING! -->
                    
                </rdf:Description>
            </xsl:for-each>
        </rdf:RDF>
    </xsl:template>
</xsl:stylesheet>
