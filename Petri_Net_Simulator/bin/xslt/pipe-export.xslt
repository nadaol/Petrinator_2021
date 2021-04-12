<?xml version="1.0" encoding="ISO-8859-1"?>
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

    <xsl:template match="/document">
        <pnml>
            <net id="Net-One" type="P/T net">

                <token id="Default" enabled="true" red="0" green="0" blue="0"/>
                <xsl:call-template name="net">

                    <xsl:with-param name="x"><xsl:value-of select="-left"/></xsl:with-param>
                    <xsl:with-param name="y"><xsl:value-of select="-top"/></xsl:with-param>

                </xsl:call-template>
            </net>
        </pnml>
    </xsl:template>

    <xsl:template name="net">

        <xsl:param name="x"/>
        <xsl:param name="y"/>

        <xsl:for-each select="place">
            <place>
                <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
                <graphics>
                    <position>
                        <xsl:attribute name="x"><xsl:value-of select="x+$x"/></xsl:attribute>
                        <xsl:attribute name="y"><xsl:value-of select="y+$y"/></xsl:attribute>
                    </position>
                </graphics>
                <name>
                    <value><xsl:value-of select="label"/></value>
                    <graphics>
                        <offset x="5" y="33"/>
                    </graphics>
                </name>
                <xsl:variable name="tokens"><xsl:value-of select="concat('Default,',tokens)"/></xsl:variable>
                <initialMarking>
                    <value>
                        <xsl:value-of select="$tokens"></xsl:value-of>
                    </value>
                </initialMarking>
                <capacity>
                    <xsl:variable name="capacity" select="'0'"/>
                    <value>
                        <xsl:value-of select="$capacity"></xsl:value-of>
                    </value>
                </capacity>
            </place>
        </xsl:for-each>

        <xsl:for-each select="transition">
            <transition>
                <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
                <graphics>
                    <position>
                        <xsl:attribute name="x"><xsl:value-of select="x+$x"/></xsl:attribute>
                        <xsl:attribute name="y"><xsl:value-of select="y+$y"/></xsl:attribute>
                    </position>
                </graphics>
                <name>
                    <value><xsl:value-of select="label"/></value>
                    <graphics>
                        <offset x="5" y="33"/>
                    </graphics>
                </name>
                <orientation>
                    <xsl:variable name="orientation" select="'270'"/>
                    <value>
                        <xsl:value-of select="$orientation"></xsl:value-of>
                    </value>
                </orientation>
                <rate>
                    <value>
                        <xsl:value-of select="rate"></xsl:value-of>
                    </value>
                </rate>
                <timed>
                    <value>
                        <xsl:value-of select="timed"></xsl:value-of>
                    </value>
                </timed>
                <infiniteServer>
                    <xsl:variable name="infiniteServer" select="'false'"/>
                    <value>
                        <xsl:value-of select="$infiniteServer"></xsl:value-of>
                    </value>
                </infiniteServer>
                <priority>
                    <xsl:variable name="priority" select="'1'"/>
                    <value>
                        <xsl:value-of select="$priority"></xsl:value-of>
                    </value>
                </priority>
            </transition>
        </xsl:for-each>

        <xsl:for-each select="arc">
            <arc>
                <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
                <xsl:attribute name="source"><xsl:value-of select="sourceId"/></xsl:attribute>
                <xsl:attribute name="target"><xsl:value-of select="destinationId"/></xsl:attribute>

                <xsl:variable name="multiplicity"><xsl:value-of select="concat('Default,',multiplicity)"/></xsl:variable>
                <inscription>
                    <value><xsl:value-of select="$multiplicity"/></value>
                    <graphics/>
                </inscription>
                <type>
                    <xsl:attribute name="value"><xsl:value-of select="type"></xsl:value-of></xsl:attribute>
                </type>
                <exported>
                    <xsl:variable name="value" select="'true'"/>
                    <xsl:value-of select="$value"></xsl:value-of>
                </exported>
                <xsl:for-each select="breakPoint">
                    <breakPoint>
                        <xsl:attribute name="x"><xsl:value-of select="x+$x"/></xsl:attribute>
                        <xsl:attribute name="y"><xsl:value-of select="y+$y"/></xsl:attribute>
                    </breakPoint>
                </xsl:for-each>
            </arc>
        </xsl:for-each>

        <xsl:for-each select="subnet">
                <xsl:call-template name="net">
                    <xsl:with-param name="x"><xsl:value-of select="x+$x"/></xsl:with-param>
                    <xsl:with-param name="y"><xsl:value-of select="y+$y"/></xsl:with-param>
                </xsl:call-template>
        </xsl:for-each>

    </xsl:template>
</xsl:transform>