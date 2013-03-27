/*
 * Copyright (c) 2013 Dror. All rights reserved
 * <p/>
 * The software source code is proprietary and confidential information of Dror.
 * You may use the software source code solely under the terms and limitations of
 * the license agreement granted to you by Dror.
 */


package dashboard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

// For date conversion to UNIX epoch format
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

// For the HTTP POST
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

// Mixpanel
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;

public class ImportCSV implements Controller
{
   public String postStatus = "";
   public String postResponse = "";
   public long t = 0;
   public String et = "not defined";
   public int errors = 0;
   
   final String GIGA_PROJECT_TOKEN = "e09c23798e34aac1991651c87770825d"; 
   //final String DROR_PROJECT_TOKEN = "f328cd5273c2986f7c51573365fed9c8"; 
   final String GIGA_API_KEY = "3e1104e06e272591b1c337c499ae7054"; 
   //final String DROR_API_KEY = "b977119eccbbe281b58c3ddee7878bf6"; 

   // ========================================
   //
   //
   // ========================================
   @Override
   public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
   {
      int n = 0;
      int lines = 0;
      
      String eventsToImport = request.getParameter("eventsToImport");
      if (eventsToImport != null)  n = Integer.parseInt(eventsToImport);
      
      // Display status window 
      ModelAndView view = new ModelAndView("hello_view");
      
      // Read the CSV log file
      //lines = importCSVlog("http://www.gigaspaces.com/downloadgen/version-checks", n);
           
      // Show summary window
      view.addObject("lines",      " >> " + lines + " sent to mixPanel.");
      view.addObject("postStatus", " >> POST status - " + postStatus + "   >> POST response -" + postResponse);
      view.addObject("timeEpoch",  " >> Event time  - " + et + ".   " + t + " in unix epoch format");

      return view;
   } 
      
   // =======================================================================
   // Read log file (CSV) and send the "Version Check" operations to Mixpanel 
   //
   //
   // =======================================================================
   public Integer importCSVlog(String u, int n)  throws Exception 
   {
      String s = "";
      Integer lines = 0;      
      URL gigaURL = new URL(u);
     
      String loginPassword = "marketing:gigaspac3s";
      String base64EncodedString = Base64.encodeBase64String(StringUtils.getBytesUtf8( loginPassword ));
      URLConnection conn = gigaURL.openConnection();
      conn.setRequestProperty ("Authorization", "Basic " + base64EncodedString);
      InputStream in = conn.getInputStream();       
         
      BufferedReader br = null;
      
    try {
      br = new BufferedReader( new InputStreamReader (in));       
   
      String inputLine = br.readLine();
      // Skip first line (headers)
      if (inputLine != null)  inputLine = br.readLine();
      
      String ip = "";
      String build = "";
      String eventTime = "3/14/13 10:10 AM";
      int x = 0;      
      // Loop - limited to n cycles (parameter defined by user)
      while (inputLine != null & lines < n)
      {
           String[] dataArray = inputLine.split(",");
           //for (String item : dataArray) s = item + "\t";
           x = 0;
           for (String ttt : dataArray) x++;
           if (x == 3) { 
           ip = dataArray[0];
           build = dataArray[1];
           eventTime = dataArray[2];
           }
           else if (x == 4) { // Line format is corrupted (2 ip's)
           errors++;
           ip = dataArray[1];
           build = dataArray[2];
           eventTime = dataArray[3];
           }
           
           //s = s + "\n"; 
           inputLine = br.readLine(); // Read next line of data.
           lines++;
           
           // Track the event in Mixpanel (using the Java API)  - event time is TODAY
           //sendEventToMixpanel(ip, "Product Activation", eventTime, build);

           // Track the event in Mixpanel (using the POST import) - event time is as indicated - in the PAST
           postCSVEventToMixpanel(ip, "Version Check", eventTime, build);
           
      } // while
    } catch (IOException e) {
         e.printStackTrace();
    } finally {   
      // Close the file once all data has been read.
      if (br != null) br.close();
      return lines;
    }
   }
   
   // =====================================================
   // Track the event in Mixpanel (using Mixpanel Java API)
   //
   // =====================================================
   public void sendEventToMixpanel(String ip, String eventName, String eventTime, String buildNumber) 
   {
   
   try {
      MessageBuilder messageBuilder = new MessageBuilder( GIGA_PROJECT_TOKEN );
      MixpanelAPI mixpanel = new MixpanelAPI();
      
      // Call Mixpanel and send the event
      Map<String, String> propMap = new HashMap<String, String>();
      propMap.put("build", buildNumber);
      propMap.put("ip", ip);
      JSONObject props = new JSONObject(propMap);
      
      // event(String distinctId, String eventName, org.json.JSONObject properties)
      JSONObject message = messageBuilder.event(ip, eventName, props);
      
      ClientDelivery delivery = new ClientDelivery();
      delivery.addMessage(message);
      mixpanel.deliver(delivery);
   
     } catch (IOException e) {
          throw new RuntimeException("Can't communicate with Mixpanel.", e);
    } 
   }
   
   // ========================================
   // Post "Version Check" event to mixpanel
   //
   // ========================================
   public void postCSVEventToMixpanel(String ip, String eventName, String eventTime, String buildNum) throws IOException 
   { 
   String pattern = "M/dd/yy h:mm a";
   SimpleDateFormat sdf  = new SimpleDateFormat(pattern);
   try {
      Date date = sdf.parse( eventTime );
      long timeInSecSinceEpoch = date.getTime() / 1000;
      t =  timeInSecSinceEpoch ;
      et = eventTime;
      
      JSONObject obj1 = new JSONObject();
      obj1.put("distinct_id", ip);
      obj1.put("ip", ip);
      obj1.put("build", buildNum);
      obj1.put("time", timeInSecSinceEpoch ); 
      obj1.put("token", GIGA_PROJECT_TOKEN);
   
      JSONObject obj2 = new JSONObject();
      obj2.put("event", eventName);
      obj2.put("properties", obj1);

      String s2 = obj2.toString();
      String encodedJSON = Base64.encodeBase64String(StringUtils.getBytesUtf8(s2));
      
      postRequest("http://api.mixpanel.com/import", "data", encodedJSON, "api_key", GIGA_API_KEY );
     } catch (Exception e) {
         throw new RuntimeException("Can't POST to Mixpanel.", e);
     }
   }
   
   // ==================================
   // postRequest
   //
   // ================================== 
   public void postRequest(String url, String p1, String v1, String p2, String v2)
   {
    try {
     String contents = ""; 
     HttpClient client = new HttpClient();
     PostMethod method = new PostMethod( url );

	   // Configure the form parameters
	   method.addParameter( p1, v1 );
	   method.addParameter( p2, v2 );
    
     // Add more details in the POST response 
	   method.addParameter( "verbose", "1" );

	   // Execute the POST method 
     int statusCode = client.executeMethod( method );
     contents = method.getResponseBodyAsString();
     
     postStatus   = Integer.toString(statusCode);
     postResponse = contents;

     method.releaseConnection();
    }
    catch( Exception e ) {
        e.printStackTrace();
    }
  }
}
