<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, CHad Berkley
*    Copyright: 2000 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*  For Details: http://www.nceas.ucsb.edu/
*
*   '$Author$'
*     '$Date$'
* '$Revision$'
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
* convert an XML file showing the resultset of a query
* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>
  <xsl:param name="cgi-prefix"/>
  <xsl:param name="contextURL"/>
  <xsl:param name="servletURL"/>  
  <xsl:param name="sessid"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="enableediting">false</xsl:param>
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css" 
              href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/common/branding.js" />
        <script language="JavaScript">
          <![CDATA[
          function submitform(action,form_ref) {
              form_ref.action.value=action;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.submit();
          }

          ]]>
        </script>
      </head>

      <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
      <script language="JavaScript">
          insertTemplateOpening("{$contextURL}");
          insertSearchBox("{$contextURL}");
      </script>
        <table width="100%" align="center" border="0" cellpadding="5" cellspacing="0">
           <tr>
             <td align="left"><br></br><p class="emphasis"><xsl:number value="count(resultset/document)" /> data packages found</p></td>
           </tr></table>
<!-- This tests to see if there are returned documents,
            if there are not then don't show the query results -->

      <xsl:if test="count(resultset/document) &gt; 0">

         <table width="95%" align="center" border="0" cellpadding="0" cellspacing="0">
           <tr>
             <th class="tablehead_lcorner" align="right" valign="top"><img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></th>
             <th class="tablehead">Title</th>
             <th width="15%" class="tablehead" style="text-align: left">Contacts</th>
             <th width="15%" class="tablehead" style="text-align: left">Organization</th>
             <th width="15%" class="tablehead" style="text-align: left">Keywords</th>
             <xsl:if test="$enableediting = 'true'">
               <th width="10%" class="tablehead" style="text-align: middle">Actions</th>
             </xsl:if>
             <th class="tablehead_rcorner" align="right" valign="top"><img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></th>
           </tr>

         <xsl:for-each select="resultset/document">
           <xsl:sort select="./param[@name='dataset/title']"/>
           <tr valign="top" class="subpanel">
             <xsl:attribute name="class">
               <xsl:choose>
                 <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                 <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
               </xsl:choose>
             </xsl:attribute>

             <td width="10">&#160;</td>
             <td class="text_plain">
               <form action="{$servletURL}" method="POST">
                 <xsl:attribute name="name">
                   <xsl:value-of select="translate(./docid, '()-.', '')" />
                 </xsl:attribute>

                 <input type="hidden" name="qformat" />
                 <input type="hidden" name="sessionid" />
                 <xsl:if test="$enableediting = 'true'">
	 	           <input type="hidden" name="enableediting" value="{$enableediting}"/>
                 </xsl:if>
                 <input type="hidden" name="action" value="read"/>
                 <input type="hidden" name="docid">
                   <xsl:attribute name="value">
                     <xsl:value-of select="./docid"/>
                   </xsl:attribute>
                 </input>
                 <xsl:for-each select="./relation">
                   <input type="hidden" name="docid">
                     <xsl:attribute name="value" >
                       <xsl:value-of select="./relationdoc" />
                     </xsl:attribute>
                   </input>
                 </xsl:for-each>

                 <a>
                   <xsl:attribute name="href">javascript:submitform('read',document.<xsl:value-of select="translate(./docid, '()-.', '')"/>)</xsl:attribute>
                   <xsl:text>&#187;&#160;</xsl:text>
                   <xsl:choose>
                     <xsl:when test="./param[@name='dataset/title']!=''">
                        <xsl:value-of select="./param[@name='dataset/title']"/>
                     </xsl:when>
                     <xsl:otherwise>
                       <xsl:value-of select="./param[@name='citation/title']"/>
                       <xsl:value-of select="./param[@name='software/title']"/>
                       <xsl:value-of select="./param[@name='protocol/title']"/>
                     </xsl:otherwise>
                   </xsl:choose>
                 </a><br />
                 <br/>
                 <p><pre>ID: <xsl:value-of select="./docid"/></pre></p>

                 <xsl:if test="starts-with(./doctype, 'eml://ecoinformatics.org/eml-2.0.0')">
                 <a>
                   <xsl:attribute name="href"><![CDATA[http://nebulous.nceas.ucsb.edu:8080/httprenderer/servlet/jalamafrontservlet?action=loadxmlinstance&url={$servletURL}%3Faction=read%26qformat=xml%26docid=]]><xsl:value-of select="./docid"/></xsl:attribute>
                   <xsl:attribute name="target">_blank</xsl:attribute>
                   <xsl:text>&#187;&#160;</xsl:text>
                   <b>Create data entry form</b>
                 </a><br/>
                 </xsl:if>

               </form>
             </td>

             <td class="text_plain">
               <xsl:for-each select="./param[@name='originator/individualName/surName']" >
                 <xsl:value-of select="." />
                 <br/>
                </xsl:for-each>
               <xsl:for-each select="./param[@name='creator/individualName/surName']" >
                 <xsl:value-of select="." />
                 <br/>
               </xsl:for-each>

             </td>
             <td class="text_plain">
                 <xsl:value-of select="./param[@name='originator/organizationName']" />
                 <xsl:value-of select="./param[@name='creator/organizationName']" />

             </td>

             <td class="text_plain">
               <xsl:for-each
                select="./param[@name='keyword']">
                 <xsl:value-of select="." />
                 <br/>
               </xsl:for-each>

             </td>
	   
             <xsl:if test="$enableediting = 'true'">
               <td class="text_plain">
	       <form action="{$servletURL}" method="POST">
	               <input type="hidden" name="action" value="read"/>
	 	       <input type="hidden" name="qformat" value="{$qformat}"/>
				<input type="hidden" name="sessionid"  value="{$sessid}"/>
                       <input type="hidden" name="docid">
                       <xsl:attribute name="value">
                          <xsl:value-of select="./docid"/>
                       </xsl:attribute>
                       </input>
                       <center>
		       <input type="SUBMIT"  value=" View " name="View">
 	               </input>
	               </center>
	             </form>
                 <form action="{$cgi-prefix}/register-dataset.cgi" 
                       method="POST">
	               <input type="hidden" name="stage" value="modify"/>	
	 	           <input type="hidden" name="cfg" value="{$qformat}"/>
					<input type="hidden" name="sessionid"  value="{$sessid}"/>
                   <input type="hidden" name="docid">
                     <xsl:attribute name="value">
                       <xsl:value-of select="./docid"/>
                     </xsl:attribute>
                   </input>
                   <center>
		             <input type="SUBMIT"  value=" Edit " name="Edit">
 	                 </input>
	               </center>
	             </form>
                 <form action="{$cgi-prefix}/register-dataset.cgi" 
                       method="POST">
	               <input type="hidden" name="stage" value="delete"/>	
	 	           <input type="hidden" name="cfg" value="{$qformat}"/>
					<input type="hidden" name="sessionid"  value="{$sessid}"/>
                   <input type="hidden" name="docid">
                     <xsl:attribute name="value">
                       <xsl:value-of select="./docid"/>
                     </xsl:attribute>
                   </input>
                   <center>
		             <input type="SUBMIT"  value="Delete" name="Delete">
 	                 </input>
	               </center>
	             </form>
	           </td>	  
             </xsl:if>
             <td width="10">&#160;</td>
             </tr>
             <tr class="searchresultsdivider"><td colspan="6">
             <img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></td></tr>

          </xsl:for-each>
          </table>

       </xsl:if>
      <script language="JavaScript">
          insertTemplateClosing("{$contextURL}");
      </script>
    </body>
    </html>
    </xsl:template>

</xsl:stylesheet>
