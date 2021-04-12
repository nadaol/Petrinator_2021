
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

<xsl:template match="/document">
<pnml>
    <net id="n-2238-D385B-0" type="Petrinator TinaDialect">
        <page id='g-2A34-1546E-1'>
        	<xsl:call-template name="subnet"><!-- use this to translate all elements to positive coordinates: --><xsl:with-param
        			name="x">
	<xsl:value-of select="-left"></xsl:value-of>
</xsl:with-param>
        		<xsl:with-param name="y">
        			<xsl:value-of select="-top"></xsl:value-of></xsl:with-param><!--instead of following 2 lines: --><!-- <xsl:with-param name="x">0</xsl:with-param>             <xsl:with-param name="y">0</xsl:with-param>--><xsl:with-param
        			name="label">
</xsl:with-param>
        	</xsl:call-template></page>
    </net></pnml>
</xsl:template>

<xsl:template name="label">
    <xsl:param name="subnetlabel"/>
    <xsl:choose>
        <xsl:when test="not(string(label))">
            <text>&#160;</text><!-- empty label -->
        </xsl:when>
        <xsl:when test="not(string($subnetlabel))">
            <text>
            	<xsl:value-of select="label"></xsl:value-of></text>
        </xsl:when>
        <xsl:otherwise>
            <text>
            	<xsl:value-of select="concat($subnetlabel,'.',label)"></xsl:value-of></text>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="subnet">
    <xsl:param name="x"/>
    <xsl:param name="y"></xsl:param>
    <xsl:param name="label"/>
    <xsl:for-each select="place">
        <place>
            <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
            <name>
                <xsl:call-template name="label">
                    <xsl:with-param name="subnetlabel"><xsl:value-of select="$label"/></xsl:with-param>
                </xsl:call-template>
                <graphics>
                    <offset x="5" y="33"></offset>
                </graphics>
            </name>
            <initialMarking>
                <text>
                	<xsl:value-of select="tokens"></xsl:value-of></text></initialMarking>
            <graphics>
                <position>
                    <xsl:attribute name="x"><xsl:value-of select="x+$x"/></xsl:attribute>
                    <xsl:attribute name="y"><xsl:value-of select="y+$y"/></xsl:attribute>
                </position>
            </graphics>
        </place>
    </xsl:for-each>
    <xsl:for-each select="transition">
        <transition>
            <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
            <name>
                <xsl:call-template name="label">
                    <xsl:with-param name="subnetlabel"><xsl:value-of select="$label"/></xsl:with-param>
                </xsl:call-template>
                <graphics>
                    <offset x="4" y="31"/>
                </graphics>
            </name>

            <xsl:if test="timed='true'">
                <delay>
                    <interval xmlns="http://www.w3.org/1998/Math/MathML" closure="closed">
                        <cn>
                            <xsl:value-of select="rate"></xsl:value-of>
                        </cn>
                        <cn>
                            <xsl:value-of select="rate"></xsl:value-of>
                        </cn>
                    </interval>
                </delay>
            </xsl:if>

            <label>
            	<text>
            		<xsl:value-of select="behavior"></xsl:value-of></text>
            	<graphics>
            		<offset x="-10" y="0"></offset></graphics></label>
            <graphics>
                <position>
                    <xsl:attribute name="x"><xsl:value-of select="x+$x"/></xsl:attribute>
                    <xsl:attribute name="y"><xsl:value-of select="y+$y"/></xsl:attribute>
                </position>
            </graphics>
        </transition>
    </xsl:for-each>
    <xsl:for-each select="arc">
        <arc>
            <xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
            <xsl:attribute name="source"><xsl:value-of select="realSourceId"/></xsl:attribute>
            <xsl:attribute name="target"><xsl:value-of select="realDestinationId"/></xsl:attribute>
            <inscription>
                <text><xsl:value-of select="multiplicity"/></text>
                <graphics>
                    <offset x="4" y="-7"/>
                </graphics>
            </inscription>
            <type>
            	<xsl:attribute name="value">
            		<xsl:value-of select="type"></xsl:value-of></xsl:attribute></type>
        </arc>
    </xsl:for-each>
    <xsl:for-each select="subnet">
        <xsl:call-template name="subnet">
            <xsl:with-param name="x"><xsl:value-of select="x+$x"/></xsl:with-param>
            <xsl:with-param name="y"><xsl:value-of select="y+$y"/></xsl:with-param>
            <xsl:with-param name="label">
                <xsl:value-of select="$label"/>
                <xsl:if test="string(label) and string($label)">.</xsl:if>
                <xsl:value-of select="label"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:for-each>
</xsl:template>

</xsl:transform>
