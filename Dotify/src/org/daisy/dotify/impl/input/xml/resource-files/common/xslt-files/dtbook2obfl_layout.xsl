<?xml version="1.0" encoding="utf-8"?>
<!--
	DTBook to Flow (sv_SE)

	Description
	Base DTBook to Flow stylesheet for Swedish.

	Parameters
		page-width
		page-height
		inner-margin
		outer-margin
		row-spacing
		duplex
		hyphenate (inherited)

	Format (input -> output)
		DTBook -> Flow

	Author: Joel Håkansson
-->
<!--
	TODO:
		- komplexa sub, sup
		- länkar, e-postadresser
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:dtb="http://www.daisy.org/z3986/2005/dtbook/" exclude-result-prefixes="dtb xs" xmlns="http://www.daisy.org/ns/2011/obfl">

	<xsl:import href="dtbook2obfl_base.xsl"/>
	<xsl:output method="xml" encoding="utf-8" indent="no"/>
	<xsl:param name="page-width" select="10" as="xs:integer"/>
	<xsl:param name="page-height" select="10" as="xs:integer"/>
	<xsl:param name="inner-margin" select="0" as="xs:integer"/>
	<xsl:param name="outer-margin" select="0" as="xs:integer"/>
	<xsl:param name="row-spacing" select="1" as="xs:decimal"/>
	<xsl:param name="duplex" select="true()" as="xs:boolean"/>
	
	<xsl:param name="l10nrearjacketcopy" select="'Rear jacket copy'"/>
	<xsl:param name="l10nimagedescription" select="'Image description'"/>
	<xsl:param name="l10ncolophon" select="'Colophon'"/>
	<xsl:param name="l10ncaption" select="'Caption'"/>
	
	<xsl:template name="insertMetadata">
		<meta xmlns:dc="http://purl.org/dc/elements/1.1/">
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dc:Title'"/>
				<xsl:with-param name="outName" select="'dc:title'"/>
			</xsl:call-template>
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dc:Creator'"/>
				<xsl:with-param name="outName" select="'dc:creator'"/>
			</xsl:call-template>
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dc:Language'"/>
				<xsl:with-param name="outName" select="'dc:language'"/>
			</xsl:call-template>
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dc:Description'"/>
				<xsl:with-param name="outName" select="'dc:description'"/>
			</xsl:call-template>
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dc:Publisher'"/>
				<xsl:with-param name="outName" select="'dc:publisher'"/>
			</xsl:call-template>
			<xsl:call-template name="addMetaElement">
				<xsl:with-param name="inName" select="'dtb:uid'"/>
				<xsl:with-param name="outName" select="'dc:source'"/>
			</xsl:call-template>
		</meta>
	</xsl:template>
	
	<xsl:template name="addMetaElement">
		<xsl:param name="inName"/>
		<xsl:param name="outName"/>
		<xsl:for-each select="/dtb:dtbook/dtb:head/dtb:meta[@name=$inName]">
			<xsl:element name="{$outName}" namespace="http://purl.org/dc/elements/1.1/">
				<xsl:value-of select="@content"/>
			</xsl:element>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="dtb:frontmatter" mode="apply-sequence-attributes">
		<xsl:attribute name="master">front</xsl:attribute>
		<xsl:attribute name="initial-page-number">1</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:bodymatter" mode="apply-sequence-attributes">
		<xsl:attribute name="master">main</xsl:attribute>
		<xsl:attribute name="initial-page-number">1</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:rearmatter" mode="apply-sequence-attributes">
		<xsl:attribute name="master">main</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h1" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">3</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level2)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
		<!--
		<xsl:if test="parent::dtb:level1/preceding-sibling::dtb:level1">
			<xsl:attribute name="break-before">page</xsl:attribute>
		</xsl:if>-->
	</xsl:template>
	<!-- If level1 has part, format h2 as h1 -->
	<xsl:template match="dtb:h2[ancestor::dtb:level1[@class='part']]" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">3</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level3)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h2" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">2</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level3)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<!-- If level1 has part, format h3 as h2 -->
	<xsl:template match="dtb:h3[ancestor::dtb:level1[@class='part']]" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">2</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level4)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h3" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level4)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h4" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level5)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h5" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:if test="(following-sibling::*[1])[not(self::dtb:level6)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:h6" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:attribute name="margin-bottom">1</xsl:attribute>
		<xsl:attribute name="keep">all</xsl:attribute>
		<xsl:attribute name="keep-with-next">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level1" mode="apply-block-attributes">
		<xsl:attribute name="break-before">page</xsl:attribute>
		<xsl:if test="not(dtb:h1)">
			<xsl:attribute name="margin-top">3</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="keep-with-previous-sheets">1</xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<!-- If level1 has part, format level2 as level1 -->
	<xsl:template match="dtb:level2[ancestor::dtb:level1[@class='part']]" mode="apply-block-attributes">
		<xsl:attribute name="break-before">page</xsl:attribute>
		<xsl:if test="not(dtb:h2)">
			<xsl:attribute name="margin-top">3</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level2" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h2)">
			<xsl:attribute name="margin-top">2</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<!-- If level1 has part, format level3 as level2 -->
	<xsl:template match="dtb:level3[ancestor::dtb:level1[@class='part']]" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h3)">
			<xsl:attribute name="margin-top">2</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level3" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h3)">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level4" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h4)">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level5" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h5)">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:level6" mode="apply-block-attributes">
		<xsl:if test="not(dtb:h6)">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:blockquote" mode="apply-block-attributes">
		<xsl:attribute name="margin-left">2</xsl:attribute>
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:attribute name="margin-bottom">1</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:list" mode="apply-block-attributes">
		<xsl:if test="not(ancestor::dtb:list)">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
		<xsl:if test="following-sibling::*[not(self::dtb:level2 or self::dtb:level3 or self::dtb:level4 or self::dtb:level5 or self::dtb:level6)]">
			<xsl:attribute name="margin-bottom">1</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="list-type"><xsl:value-of select="@type"/></xsl:attribute>
			<xsl:choose>
				<xsl:when test="ancestor::dtb:list"><!--<xsl:attribute name="margin-left">3</xsl:attribute>--></xsl:when>
				<xsl:otherwise><xsl:attribute name="margin-left">2</xsl:attribute></xsl:otherwise>
			</xsl:choose>
	</xsl:template>
	<xsl:template match="dtb:li" mode="apply-block-attributes">
		<xsl:choose>
			<xsl:when test="parent::dtb:list/@type='pl'">
				<xsl:variable name="indent_max" select="max((parent::dtb:list/dtb:li)/string-length(substring-before(descendant::text()[1], ' '))) + 1"/>
				<xsl:variable name="indent_min" select="min((parent::dtb:list/dtb:li)/string-length(substring-before(descendant::text()[1], ' '))) + 1"/>
				<xsl:variable name="indent" select="if ($indent_min=$indent_max and $indent_min &lt; 5) then $indent_min else 3"/>
				<xsl:attribute name="text-indent"><xsl:value-of select="$indent"/></xsl:attribute>
				<xsl:attribute name="block-indent"><xsl:value-of select="$indent"/></xsl:attribute>
			</xsl:when>
			<xsl:when test="parent::dtb:list/@type='ul'">
				<xsl:attribute name="first-line-indent">2</xsl:attribute>
				<xsl:attribute name="text-indent">2</xsl:attribute>
				<xsl:attribute name="block-indent">2</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="first-line-indent">3</xsl:attribute>
				<xsl:attribute name="text-indent">3</xsl:attribute>
				<xsl:attribute name="block-indent">3</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="dtb:div[@class='pgroup']" mode="apply-block-attributes">
		<xsl:attribute name="margin-bottom">1</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:poem" mode="apply-block-attributes">
		<xsl:attribute name="margin-top">1</xsl:attribute>
		<xsl:attribute name="margin-bottom">1</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:line" mode="apply-block-attributes">
		<xsl:attribute name="text-indent">2</xsl:attribute>
	</xsl:template>
	<xsl:template match="dtb:linegroup" mode="apply-block-attributes">
		<xsl:if test="(preceding-sibling::*[1])[self::dtb:linegroup or self::dtb:pagenum[(preceding-sibling::*[1])[self::dtb:linegroup]]]">
			<xsl:attribute name="margin-top">1</xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	<!-- Don't output a sequence if there is nothing left when doctitle, docauthor and level1@class='backCoverText', level1@class='rearjacketcopy' and level1@class='colophon' has been moved -->
	<xsl:template match="dtb:frontmatter" mode="sequence-mode">
		<xsl:if test="*[not(self::dtb:doctitle or self::dtb:docauthor or self::dtb:level1[@class='backCoverText' or @class='rearjacketcopy' or @class='colophon'])]">
			<sequence>
				<xsl:apply-templates select="." mode="apply-sequence-attributes"/>
				<xsl:apply-templates/>
			</sequence>
		</xsl:if>
	</xsl:template>
	
	<!-- 	Don't output a sequence if there is nothing left when 
			level1@class='backCoverText', level1@class='rearjacketcopy' and @class='colophon' has been moved.
			Assume that bodymatter contains something besides this, don't even test it.
	-->
	<xsl:template match="dtb:rearmatter" mode="sequence-mode">
		<xsl:if test="*[not(self::dtb:level1[@class='backCoverText' or @class='rearjacketcopy' or @class='colophon'])]">
			<sequence>
				<xsl:apply-templates select="." mode="apply-sequence-attributes"/>
				<xsl:apply-templates/>
			</sequence>
		</xsl:if>
	</xsl:template>

	<!-- Exclude docauthor and doctitle in frontmatter. These will be inserted on a title page later. -->
	<xsl:template match="dtb:doctitle[parent::dtb:frontmatter] | dtb:docauthor[parent::dtb:frontmatter]"></xsl:template>
	<!-- Exclude here, these are organized below. -->
	<xsl:template match="dtb:level1[@class='colophon']"></xsl:template>
	<xsl:template match="dtb:level1[@class='backCoverText' or @class='rearjacketcopy']"></xsl:template>
	
	<!-- Organize colophon and rearjacketcopy -->
	<xsl:template match="dtb:book" priority="10">
		<xsl:apply-templates/>
		<xsl:for-each select="//dtb:level1[@class='colophon']">
			<sequence master="plain" initial-page-number="1">
				<block margin-bottom="1"><xsl:value-of select="concat(':: ', $l10ncolophon, ' ')"/><leader position="100%" pattern=":"/></block>
				<block>
					<xsl:apply-templates select="node()"/>
				</block>
				<block><leader position="100%" pattern=":"/></block>
			</sequence>
		</xsl:for-each>
		<xsl:for-each select="//dtb:level1[@class='backCoverText' or @class='rearjacketcopy']">
			<sequence master="plain" initial-page-number="1">
				<block margin-bottom="1"><xsl:value-of select="concat(':: ', $l10nrearjacketcopy, ' ')"/><leader position="100%" pattern=":"/></block>
				<block>
					<xsl:apply-templates select="node()"/>
				</block>
				<block><leader position="100%" pattern=":"/></block>
			</sequence>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Override default processing -->
	<xsl:template match="dtb:prodnote[ancestor::dtb:imggroup]" priority="10">
		<block keep="all" keep-with-next="1"><xsl:value-of select="concat(':: ', $l10nimagedescription, ' ')"/><leader position="100%" pattern=":"/></block>
		<block>
			<xsl:apply-templates/>
		</block>
		<block><leader align="right" position="100%" pattern=":"/></block>
	</xsl:template>
	
	<!-- Override default processing -->
	<xsl:template match="dtb:caption[ancestor::dtb:imggroup]" priority="10">
		<block keep="all" keep-with-next="1"><xsl:value-of select="concat(':: ', $l10ncaption, ' ')"/><leader position="100%" pattern=":"/></block>
		<block>
			<xsl:apply-templates/>
		</block>
		<block><leader position="100%" pattern=":"/></block>
	</xsl:template>

	<!-- Override default processing -->
	<xsl:template match="dtb:p" mode="block-mode" priority="10">
		<xsl:variable name="tokens" select="tokenize(@class,' ')"/>
		<xsl:if test="$tokens='precedingseparator'">
			<block keep="all" keep-with-next="1" margin-top="1" margin-bottom="1"><xsl:text>---</xsl:text></block>
		</xsl:if>
		<block>
			<xsl:if test="$tokens='precedingemptyline'">
				<xsl:attribute name="margin-top">1</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="$tokens='indented'"><xsl:attribute name="first-line-indent">2</xsl:attribute></xsl:when>
				<xsl:when test="not($tokens='precedingemptyline' or $tokens='precedingseparator' or $tokens='no-indent')">
					<xsl:if test="(preceding-sibling::*[1])[self::dtb:p or self::dtb:pagenum[(preceding-sibling::*[1])[self::dtb:p]]]">
						<xsl:attribute name="first-line-indent">2</xsl:attribute>
					</xsl:if>
				</xsl:when>
			</xsl:choose>
			<xsl:apply-templates/>
		</block>
	</xsl:template>

	<xsl:template match="dtb:lic[
				not(
					following-sibling::node()[
						not(self::dtb:list) and not(self::text() and normalize-space()='')
					]
				) 
				and ancestor::dtb:*[@class='toc'] and preceding-sibling::dtb:lic
			]" mode="inline-mode">
		<leader position="100%" pattern="." align="right"/><xsl:apply-templates/>
	</xsl:template>

	<!-- Override default processing -->
	<xsl:template match="dtb:dt">
		<xsl:apply-templates select="." mode="block-mode"/>
	</xsl:template>

	<!-- Override default processing -->
	<xsl:template match="dtb:dd">
		<xsl:apply-templates select="." mode="block-mode"/>
	</xsl:template>

	<!-- Override default processing -->
	<xsl:template match="dtb:h1[parent::dtb:level1/@class='part']">
		<block><xsl:apply-templates select="." mode="apply-block-attributes"/>
			<xsl:attribute name="keep-with-next-sheets">1</xsl:attribute>
			<block><leader position="100%" align="right" pattern=":"/></block>
			<block margin-left="2" margin-right="2" text-indent="2"><xsl:apply-templates/></block>
			<block><leader position="100%" align="right" pattern=":"/></block>
		</block>
	</xsl:template>
	
	<xsl:template match="dtb:dd" mode="apply-block-attributes">
		<xsl:attribute name="text-indent">3</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="text()">
		<xsl:choose>
			<xsl:when test="ancestor::dtb:*[@xml:lang][1][not(self::dtb:dtbook)]">
				<span><xsl:attribute name="xml:lang"><xsl:value-of select="ancestor::dtb:*[@xml:lang][1]/@xml:lang"/></xsl:attribute><xsl:value-of select="."/></span>
			</xsl:when>
			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
