<html>
  <body bgcolor="white">
    <div style="font-size: 100%; color: #850F0F">
      <span>Enter number of lines to import from CSV file: </span><br />
      <form method="post" action="hello">
        <input type=text size="25" name="eventsToImport" >
        <input type=submit name="submit" value="Import">
      </form>
    </div>
    <div>
      <%
          {
            java.lang.String lines = (java.lang.String)request.getAttribute("lines");   
            java.lang.String postStatus = (java.lang.String)request.getAttribute("postStatus");   
            java.lang.String timeEpoch = (java.lang.String)request.getAttribute("timeEpoch");   
            
      %>
      <span><%=lines%></span><BR>
      <span><%=postStatus%></span><BR>
      <span><%=timeEpoch%></span><BR>
      <BR>Time <%= new java.util.Date() %>
      <%
          }
      %>
    </div>
  </body>
</html>
