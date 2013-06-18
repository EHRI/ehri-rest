<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output indent="yes" encoding="UTF-8" />
    <xsl:template match="/">
        <xsl:for-each select="item">
            <doc>
                <xsl:variable name="documentId" select="@id"/>
                <xsl:variable name="holderId" select="relationships/heldBy/item[1]/@id" />
                <xsl:variable name="holderName" select="relationships/heldBy/item[1]/data/property[@name='name']" />
                <xsl:variable name="lastUpdated"  select="relationships/lifecycleEvent/item/data/property[@name='timestamp']" />
                <xsl:for-each select="relationships/describes/item">
                    <field name="id"><xsl:value-of select="@id" /></field>
                    <field name="type">documentDescription</field>
                    <field name="itemId"><xsl:value-of select="$documentId" /></field>
                    <field name="holderId"><xsl:value-of select="$holderId" /></field>
                    <field name="holderName"><xsl:value-of select="$holderName" /></field>
                    <field name="lastUpdated"><xsl:value-of select="$lastUpdated" /></field>
                    <xsl:for-each select="data/property">
                        <xsl:variable name="propertyName" select="@name"/>
                        <field name="{$propertyName}"><xsl:value-of select="." /></field>
                    </xsl:for-each>
                    <xsl:for-each select="data/propertySequence/property">
                        <xsl:variable name="propertyName" select="@name"/>
                        <field name="{$propertyName}"><xsl:value-of select="." /></field>
                    </xsl:for-each>
                    <xsl:for-each select="relationships/hasDate">
                        <field name="datePeriod"><xsl:value-of select="." /></field>
                    </xsl:for-each>
                </xsl:for-each>
                <xsl:for-each select="relationships/access/item">
                    <field name="accessibleTo"><xsl:value-of select="@id" /></field>
                </xsl:for-each>
            </doc>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
