package com.ibm.watson;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.wink.json4j.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Path("/getsentiment")
public class SentimentAnalysis {
	String ORIGINAL_SERVICE_URL = "https://access.alchemyapi.com";
	String xmlHeader = "<?xml version='1.0' encoding='UTF-8'?>";
	String apiURL = "/calls/text/TextGetTextSentiment";
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_XML)
	public String TextGetTextSentiment(@FormParam("text") String text,
			@FormParam("url") String url,
			@FormParam("showSourceText") String showSourceText) {

		HttpURLConnection conn = null;
		DocumentBuilderFactory xmlBuilderFactory;
		DocumentBuilder xmlBuilder;
		

		try {
			xmlBuilderFactory = DocumentBuilderFactory.newInstance();
			xmlBuilder = xmlBuilderFactory.newDocumentBuilder();
		} catch (Exception e) {
			return xmlHeader + "<results><status>fatal</status><statusInfo>" + e.toString() + "</statusInfo></results>";
		}

		try {
			// Get the service endpoint details

			// 'VCAP_APPLICATION' is in JSON format, it contains useful
			// information
			// about a deployed application
			// 'VCAP_SERVICES' contains all the credentials of services bound to
			// this application.
			// String VCAP_SERVICES = System.getenv("VCAP_SERVICES");

			// Find my service from VCAP_SERVICES in BlueMix
			JSONObject serviceInfo = new JSONObject(
					System.getenv("VCAP_SERVICES"));

			// Get the Service Credentials for Watson AlchemyAPI
			String Service_Name = "user-provided";
			JSONObject credentials = serviceInfo.getJSONArray(Service_Name)
					.getJSONObject(0).getJSONObject("credentials");

			try {
				serviceURL = credentials.getString("url");
			} catch (Exception e) {
			}
			// If we didn't find a URL for the AlchemyAPI service in
			// VCAP_SERVICES,
			// use the original.
			if ("".equals(serviceURL)) {
				serviceURL = ORIGINAL_SERVICE_URL;
			}
			String apikey = credentials.getString("apikey");

			// Prepare the HTTP connection to the service
			byte[] requestData = buildHttpPostData(text, apikey, url, showSourceText);
			conn = (HttpURLConnection) new URL(serverURL + apiURL)
					.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length",
					String.valueOf(requestData.length));
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			// make the connection
			conn.connect();
			
			conn.getOutputStream().write(requestData);
			conn.getOutputStream().flush();
			

			// Read the response from the service
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			br.close();
			//System.out.println("The result: " + sb.toString());

			// Return the response from the service
			return sb.toString();
			

		} catch (Exception e) {
			try {
				//System.out.println("Result: "
					//	+ e
						//+ "XML is "
						//+ ConceptTagging.buildXmlResponse(xmlBuilder,
							//	"exception", e.getClass().getName(), null));
				// Return the exception in XML format
				return xmlHeader + SentimentAnalysis.buildXmlResponse(xmlBuilder, "exception",
						e.getClass().getName());
			} catch (Exception e1) {
				return xmlHeader + "<results><status>fatal</status><statusInfo>" + e1.toString() + "</statusInfo></results>";
			}
		} finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			} catch (Exception e) {
				return xmlHeader + "<results><status>fatal</status><statusInfo>" + e.toString() + "</statusInfo></results>";
			}
		}

	}

	// Build the XML response
	private static String buildXmlResponse(DocumentBuilder xmlBuilder,
			String status, String statusInfo) throws Exception {
		Document xml = xmlBuilder.newDocument();

		Element root = xml.createElement("results");
		xml.appendChild(root);

		Element statusNode = xml.createElement("status");
		statusNode.appendChild(xml.createTextNode(status));
		root.appendChild(statusNode);

		Element statusInfoNode = xml.createElement("statusInfo");
		statusInfoNode.appendChild(xml.createTextNode(statusInfo));
		root.appendChild(statusInfoNode);
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(xml);
		StringWriter resultBuffer = new StringWriter();
		StreamResult transformResult = new StreamResult(resultBuffer);
		transformer.transform(source, transformResult);

		return resultBuffer.toString();
	}

	// Build HTTP Post
	// Provide the default value for some parameters
	private static byte[] buildHttpPostData(String text, String serviceApiKey,
			String url, String showSourceText) throws UnsupportedEncodingException {
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put("text", text);
		parameterMap.put("apikey", serviceApiKey);
		parameterMap.put("url", url);
		if (showSourceText == null) {
			parameterMap.put("showSourceText", 0);
		} else {
			parameterMap.put("showSourceText", showSourceText);
		}


		return SentimentAnalysis.encodeHttpPostParameters(parameterMap);
	}

	// Encode HTTP Post parameters
	 private static byte[] encodeHttpPostParameters(Map<String, Object> params)
	   throws UnsupportedEncodingException {
	  StringBuilder postData = new StringBuilder();
	  for (Map.Entry<String, Object> param : params.entrySet()) {
	   if (postData.length() != 0)
	    postData.append('&');
	   postData.append(param.getKey());
	   postData.append('=');
	   
	   postData.append(URLEncoder.encode(String.valueOf(param.getValue()),
	     "UTF-8"));
	  }
	  return postData.toString().getBytes("UTF-8");
	 }
	}