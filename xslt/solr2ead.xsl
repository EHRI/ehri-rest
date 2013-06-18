<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">
    <xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>
    <xsl:output encoding="UTF-8"/>

<!--
//*****************************************************************************
// Copyright 2013 by Junte Zhang <junte.zhang@meertens.knaw.nl>
// Distributed under the GNU General Public Licence
//*****************************************************************************
-->

    <xsl:template match="/add">
        <xsl:for-each select="//doc">
            <xsl:variable name="parent" select="field[@name = 'assoc_parent_irn']/normalize-space()" />
            <xsl:if test="$parent != ''">
                <xsl:variable name="filename" select="concat('ead/', $parent, '/' , field[@name = 'id'] , '.xml')" />
                <xsl:result-document href="{$filename}" method="xml">
                    <ead>
                        <xsl:call-template name="header"/>
                        <xsl:call-template name="fm"/>
                        <xsl:call-template name="description"/>
                        <xsl:apply-templates />
                    </ead>
                </xsl:result-document>
            </xsl:if>
            <xsl:if test="not($parent)">
                <xsl:variable name="filename" select="concat('ead/', field[@name = 'id'] , '.xml')" />
                <xsl:result-document href="{$filename}" method="xml">
                    <ead>
                        <xsl:call-template name="header"/>
                        <xsl:call-template name="fm"/>
                        <xsl:call-template name="description"/>
                        <xsl:apply-templates />
                    </ead>
                </xsl:result-document>
            </xsl:if>

        </xsl:for-each>
    </xsl:template>

    <!-- header: <eadheader> -->
        <xsl:template name="header">
            <eadheader>
                <eadid>
                    <xsl:value-of select="field[@name = 'id']/normalize-space()" />
                </eadid>
                <filedesc>
                    <titlestmt>
                        <titleproper>
                            <xsl:value-of select="field[@name = 'collection_name']/normalize-space()" />
                        </titleproper>
                        <subtitle>
                            <xsl:value-of select="field[@name = 'title']/normalize-space()" />
                        </subtitle>
                        <author>
                            <xsl:value-of select="field[@name = 'creator_name']/normalize-space()" />
                        </author>
                    </titlestmt>
                    <publicationstmt>
                        <publisher>
                        </publisher>
                        <date calendar="gregorian" era="ce"></date>
                    </publicationstmt>
                </filedesc>
                <profiledesc>
                    <creation>
                        <xsl:value-of select="field[@name = 'datetimemodified']/normalize-space()" />
                    </creation>
                    <langusage>
                        <xsl:value-of select="field[@name = 'language']/normalize-space()" />
                    </langusage>
                </profiledesc>
            </eadheader>
        </xsl:template>

        <!-- frontmatter: <frontmatter> -->
            <xsl:template name="fm">
                <frontmatter>
                    <titlepage>
                        <titleproper>
                            <xsl:value-of select="field[@name = 'collection_name']/normalize-space()" />
                        </titleproper>
                        <publisher>
                        </publisher>
                        <date calendar="gregorian" era="ce">
                            <xsl:value-of select="field[@name = 'display_date']/normalize-space()" />
                        </date>
                    </titlepage>
                </frontmatter>
            </xsl:template>

            <!-- archival description: <archdesc> -->
                <xsl:template name="description">
                    <archdesc>
                        <did>
                            <unitid>
                                <xsl:value-of select="field[@name = 'irn']/normalize-space()" />
                            </unitid>
                            <unittitle>
                                <xsl:value-of select="field[@name = 'title']/normalize-space()" />
                            </unittitle>
                            <origination>
                                <xsl:value-of select="field[@name = 'provenance']/normalize-space()" />
                            </origination>
                            <unitdate calendar="gregorian" era="ce">
                                <xsl:value-of select="field[@name = 'display_date']/normalize-space()" />
                            </unitdate>
                            <physdesc>
                                <extent>
                                    <xsl:value-of select="field[@name = 'extent']/normalize-space()" />
                                </extent>
                                <physfacet>
                                    <xsl:value-of select="field[@name = 'dimensions']/normalize-space()" />
                                    <xsl:value-of select="field[@name = 'material_composition']/normalize-space()" />
                                </physfacet>
                            </physdesc>
                            <repository>

                            </repository>
                            <abstract>
                                <xsl:for-each select="field[@name = 'brief_desc']">
                                    <p>
                                        <xsl:value-of select="./normalize-space()" />
                                    </p>
                                </xsl:for-each>
                            </abstract>
                        </did>

                        <acqinfo>
                            <xsl:variable name="accession" select="field[@name = 'accession_number']/normalize-space()" />
                            <xsl:variable name="source" select="distinct-values(field[@name = 'acq_source']/normalize-space())" /> 
                            <xsl:variable name="credit" select="field[@name = 'acq_credit']/normalize-space()" />
                            <xsl:choose> 
                                <xsl:when test="$accession != ''">
                                    <p>Accession number: <xsl:copy-of select="$accession" /></p>
                                </xsl:when>
                            </xsl:choose>
                            <xsl:choose> 
                                <xsl:when test="$source != ''">
                                    <p>Source: <xsl:copy-of select="$source" /></p>
                                </xsl:when>
                            </xsl:choose>
                            <xsl:choose> 
                                <xsl:when test="$credit != ''">
                                    <p>Credit: <xsl:copy-of select="$credit" /></p>
                                </xsl:when>
                            </xsl:choose>
                        </acqinfo>

                        <!-- biographic description of the person or organization -->
                        <bioghist>
                            <xsl:for-each select="field[@name = 'creator_bio']">
                                <p>
                                    <xsl:value-of select="./normalize-space()" />
                                </p>
                            </xsl:for-each>
                        </bioghist>

                        <!-- a detailed narrative description of the collection material -->
                        <scopecontent>
                            <xsl:for-each select="field[@name = 'scope_content']">
                                <p>
                                    <xsl:value-of select="./normalize-space()" />
                                </p>
                            </xsl:for-each>
                        </scopecontent>

                        <!-- description of items which the repository acquired separately but which are related to this collection, and which a researcher might want to be aware of -->
                        <relatedmaterial>

                        </relatedmaterial>

                        <userestrict>
                            <xsl:for-each select="field[@name = 'copyright']">
                                <p>
                                    <xsl:value-of select="./normalize-space()" />
                                </p>
                            </xsl:for-each>
                        </userestrict>

                        <!-- items which the repository acquired as part of this collection but which have been separated from it, perhaps for special treatment, storage needs, or cataloging -->
                        <separatedmaterial>
                        </separatedmaterial>

                        <!-- a list of subject headings or keywords for the collection, usually drawn from an authoritative source such as Library of Congress Subject Headings or the Art and Architecture Thesaurus
accessrestrict and userestrict - statement concerning any restrictions on the material in the collection -->
            <controlaccess>
                <xsl:for-each select="field[@name = 'subject_type']">
                    <xsl:choose>
                        <xsl:when test=". = 'Personal Name'">
                            <xsl:variable name = "pos" select="position()" />
                            <p>
                                <persname>
                                    <xsl:value-of select="../field[@name = 'subject_heading'][$pos]/normalize-space()" />
                                </persname>
                            </p>
                        </xsl:when>
                        <xsl:when test=". = 'Corporate Name'">
                            <xsl:variable name = "pos" select="position()" />
                            <p>
                                <corpname>
                                    <xsl:value-of select="../field[@name = 'subject_heading'][$pos]/normalize-space()" />
                                </corpname>
                            </p>
                        </xsl:when>
                        <xsl:when test=". = 'Topical Term'">
                            <xsl:variable name = "pos" select="position()" />
                            <p>
                                <subject>
                                    <xsl:value-of select="../field[@name = 'subject_heading'][$pos]/normalize-space()" />
                                </subject>
                            </p>
                        </xsl:when>
                        <xsl:otherwise>
                            <p>
                                <xsl:value-of select="../field[@name = 'subject_heading']/normalize-space()" />
                            </p>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </controlaccess>

            <!-- second part of the archival description: the inventory with descriptive subordinate components -->
            <dsc>
            </dsc>
        </archdesc>
    </xsl:template>

    <xsl:template match="text()|@*"/>

</xsl:stylesheet>
