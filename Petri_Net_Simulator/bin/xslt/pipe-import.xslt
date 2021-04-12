<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2017 Leandro Asson leoasson at gmail.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

    <xsl:template match="/">
        <document>
            <subnet>
                <id>0</id>
                <x>0</x>
                <y>0</y>
                <xsl:call-template name="subnet"/>
            </subnet>
        </document>
    </xsl:template>

    <xsl:template name="subnet">
        <xsl:for-each select="pnml/net/place">
            <place>
                <id><xsl:value-of select="@id"/></id>
                <x><xsl:value-of select="floor(graphics/position/@x)"/></x>
                <y><xsl:value-of select="floor(graphics/position/@y)"/></y>
                <label><xsl:value-of select="name/value"/></label>

                <xsl:variable name="tokens"><xsl:value-of select="substring(initialMarking/value,1,8)"/></xsl:variable>
                <xsl:choose>
                    <xsl:when test="$tokens = 'Default,'">
                        <tokens> <xsl:value-of select="substring(initialMarking/value,9,string-length((initialMarking/value))-8)"/></tokens>
                    </xsl:when>
                    <xsl:otherwise>
                        <tokens> <xsl:value-of select="initialMarking/value"/></tokens>
                    </xsl:otherwise>
                </xsl:choose>
                <isStatic>false</isStatic>
            </place>
        </xsl:for-each>

        <xsl:for-each select="pnml/net/transition">
            <transition>
                <id><xsl:value-of select="@id"/></id>
                <x><xsl:value-of select="floor(graphics/position/@x)"/></x>
                <y><xsl:value-of select="floor(graphics/position/@y)"/></y>
                <label><xsl:value-of select="name/value"/></label>
                <timed><xsl:value-of select="timed"/></timed>
                <rate><xsl:value-of select="rate"/></rate>
                <automatic>false</automatic>
                <informed>true</informed>
                <enableWhenTrue>false</enableWhenTrue>
                <guard>none</guard>
            </transition>
        </xsl:for-each>

        <xsl:for-each select="pnml/net/arc">
            <arc>
                <sourceId><xsl:value-of select="@source"/></sourceId>
                <destinationId><xsl:value-of select="@target"/></destinationId>

                <xsl:variable name="Multiplicity"><xsl:value-of select="substring(inscription/value,1,8)"/></xsl:variable>
                <xsl:choose>
                    <xsl:when test="$Multiplicity = 'Default,'">
                        <multiplicity> <xsl:value-of select="substring(inscription/value,9,string-length((inscription/value))-8)"/></multiplicity>
                    </xsl:when>
                    <xsl:otherwise>
                        <multiplicity> <xsl:value-of select="inscription/value"/></multiplicity>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:variable name="isExported"><xsl:value-of select="exported"/></xsl:variable>
                <xsl:choose>
                    <xsl:when test="$isExported = 'true'">
                        <xsl:for-each select="breakPoint">
                            <breakPoint>
                                <x><xsl:value-of select="@x"/></x>
                                <y><xsl:value-of select="@y"/></y>
                            </breakPoint>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:for-each select="arcpath">
                            <xsl:if test="position()!= 1 and position() != last()">
                                <breakPoint>
                                    <x><xsl:value-of select="@x"/></x>
                                    <y><xsl:value-of select="@y"/></y>
                                </breakPoint>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:otherwise>
                </xsl:choose>
                 <type><xsl:value-of select="type/@value"/></type>
             </arc>
         </xsl:for-each>

    </xsl:template>
 </xsl:transform>
