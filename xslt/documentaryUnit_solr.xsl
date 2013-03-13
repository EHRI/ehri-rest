<?xml version="1.0" encoding="UTF-8"?>
<!-- produce xml for use with Solr 4.1 -->
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                exclude-result-prefixes="xsl xs">
    
    <xsl:output indent="yes" encoding="UTF-8" />
    <xsl:strip-space elements="*"/>
    
    <!-- Note: Could not get xsl:function working on several 2.0 engines, 
    so considered it an unstable feature and using a named template instead-->
    <xsl:template name="ehri_textFieldName">
        <xsl:param name="langCode"/>
        <xsl:variable name="alpha2" select="substring($langCode,1,2)"/>
        <!-- TODO only a given set of codes are possible, 
        otherwise fall back to 'text'. 
        or we could have the solr schema.xml handle text_* -->
        <xsl:value-of select="concat('text_', $alpha2)"/>
    </xsl:template>

    <xsl:template match="text()"><xsl:value-of select="normalize-space(.)"/></xsl:template>
        
    <xsl:template match="/list">
        <add>
        <xsl:for-each select="item">

                <xsl:variable name="documentId" select="@id"/>
                <!--  Maybe also the doc name and identifier ? -->
                <xsl:variable name="holderId" select="relationships/heldBy/item[1]/@id" />
                <xsl:variable name="lastUpdated"  select="relationships/lifecycleEvent/item/data/property[@name='timestamp']" />
                <xsl:variable name="doctype" select="@type"/>
                <xsl:for-each select="relationships/describes/item">
                    <xsl:variable name="languageCode" select="data/property[@name='languageCode']"/>
                    <!-- use the languagecode 
                    for indexing in language specific fields -->
                    <xsl:variable name="textFieldNameForLanguage">
                        <xsl:call-template name="ehri_textFieldName" >
                            <xsl:with-param name="langCode" select="$languageCode"/>
                        </xsl:call-template>
                    </xsl:variable>                   
                    <doc>
                        <field name="id"><xsl:value-of select="@id" /></field> 
                        
                        <!-- using dynamic fields, so we don't need to change the schema -->
                        
                        <field name="documentId_s"><xsl:value-of select="$documentId" /></field>
                        <field name="type_s"><xsl:value-of select="$doctype" /></field>
                        <field name="lang_s"><xsl:value-of select="$languageCode" /></field>
                        <field name="holderId_s"><xsl:value-of select="$holderId" /></field>
                        <xsl:for-each select="../../access/item">
                            <!-- If no *_ss in your schema.xml, you could now use *_txt -->
                            <field name="accessibleTo_ss"><xsl:value-of select="@id" /></field>
                        </xsl:for-each>
                        
                        <xsl:if test="$lastUpdated">
                            <field name="lastUpdated_dt"><xsl:value-of select="$lastUpdated" /></field>
                        </xsl:if>
                        <!-- skip these, if no *_dts  in our schema.xml -->
                        <xsl:for-each select="relationships/hasDate">
                            <field name="datePeriod_dts"><xsl:value-of select="." /></field>
                        </xsl:for-each>
                        
                        <!-- all properties -->
                        <!-- Also using dynamic fields, so we don't need to change the schema -->

                        <xsl:for-each select="data/property">
                            <!-- languageCode is in here as well, skip it -->
                            <xsl:if test="@name!='languageCode'">
                                <xsl:variable name="propertyName" select="@name"/>
                                <field name="{$propertyName}_txt"><xsl:value-of select="." /></field>
                                <!-- use the languagecode for indexing in language specific text field -->
                                <field name="{$textFieldNameForLanguage}"><xsl:value-of select="." /></field> 
                                <!-- also put it in generic field, and not via a copyField in the schema  -->
                                <field name="text"><xsl:value-of select="." /></field>
                            </xsl:if>
                        </xsl:for-each>
                        
                        <xsl:for-each select="data/propertySequence/property">
                            <xsl:variable name="propertyName" select="@name"/>
                            <field name="{$propertyName}_txt"><xsl:value-of select="." /></field>
                            <!-- use the languagecode for indexing in language specific text field -->
                            <field name="{$textFieldNameForLanguage}"><xsl:value-of select="." /></field> 
                            <!-- also put it in generic field, and not via a copyField in the schema  -->
                            <field name="text"><xsl:value-of select="." /></field>
                        </xsl:for-each>
                        
                    </doc>
                </xsl:for-each>
                
        </xsl:for-each>
        </add>
    </xsl:template>
</xsl:stylesheet>