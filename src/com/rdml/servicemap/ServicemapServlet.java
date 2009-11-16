package com.rdml.servicemap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

@SuppressWarnings("serial")
public class ServicemapServlet extends HttpServlet {
	
	public static final String USERNAME = "YOUR_ZOHO_USERNAME";
	public static final String PASSWORD = "YOUR_ZOHO_PASSWORD";
	public static final String gMapApiKey = "YOUR_GOOGLE_MAPS_API_KEY";
	public static final String zohoApiKey = "YOUR_ZOHO_API_KEY";
	
	private class Lead {
		private String companyName;
		private String street;
		private String city;
		private String country;
		
		public String getCompanyName() {
			return companyName;
		}
		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}
		public String getStreet() {
			return street;
		}
		public void setStreet(String street) {
			this.street = street;
		}
		public String getCity() {
			return city;
		}
		public void setCity(String city) {
			this.city = city;
		}
		public String getCountry() {
			return country;
		}
		public void setCountry(String country) {
			this.country = country;
		}
	}
	
	private Cache cache = null; 
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		if (cache == null) {
		    try {
		        Map props = new HashMap();
		        props.put(GCacheFactory.EXPIRATION_DELTA, 3600);
	            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
	            cache = cacheFactory.createCache(props);
	        } catch (CacheException e) {
	            // ...
	        }			
		}

		String ticket = getIAmTicket("ZohoCRM", USERNAME, PASSWORD);
	    String targetURL = "https://crm.zoho.com/crm/private/xml/Leads/getAllRecords?apikey=" + zohoApiKey + "&" + "ticket=" + ticket;

	    String json = null;
		try {
			URL url = new URL(targetURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				List<Lead> leads = getLeads(connection.getInputStream());
				json = printJsonMarkers(leads);
			} else {
				//
			}
		} catch (MalformedURLException e) {
			resp.getWriter().println("Bad URL.");
		} catch (IOException e) {
			resp.getWriter().println("I/O Error.");
		}
		
		req.setAttribute("jsonMarkers", json);
		req.setAttribute("gMapApiKey", gMapApiKey);
		RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/index.jsp");
		try {
			rd.forward(req, resp);
		} catch (ServletException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}
	
	public String geoCode(Lead l) {

		String queryString = l.getStreet() + " " + l.getCity() + " " + l.getCountry();

		if (cache.containsKey(queryString))
			return (String) cache.get(queryString);
		
		StringBuilder sb = new StringBuilder();
		try {
			String targetURL = "http://maps.google.com/maps/geo?q=" + URLEncoder.encode(queryString, "UTF-8") + "&output=json&sensor=false&key=" + gMapApiKey;
			URL url = new URL(targetURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
            while ((line = reader.readLine()) != null) {
            	sb.append(line);
            }
            
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				cache.put(queryString, sb.toString());
			} else {
				System.err.println(sb.toString());
			}
		} catch (MalformedURLException e) {
			//
		} catch (IOException e) {
			//
		}
				
		return sb.toString();		
	}

	private String printJsonMarkers(List<Lead> leads) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Lead l : leads) {
			sb.append(geoCode(l));
			if (leads.indexOf(l) < leads.size()-1)
				sb.append(",");
		}
		sb.append("]");
		
		return sb.toString();		
	}

	private List<Lead> getLeads(InputStream inputStream) throws IOException {
		Document doc = null;
		try {
	  	    Builder parser = new Builder();
		    doc = parser.build(inputStream);
		} catch (ParsingException ex) {
			System.err.println("Couldn't parse response XML.");
		} catch (IOException ex) {
		    System.err.println("Could not connect to Zoho.");
		}
			
        Element root = doc.getRootElement();
        Nodes rows = root.query("//row");
        
        List<Lead> leads = new ArrayList<Lead>();
        
        for (int i = 0; i < rows.size(); i++) {
        	Node row = rows.get(i);
        	if (row instanceof Element) {
        		Lead l = new Lead();
    			leads.add(l);
        		Element e = (Element) row;
        		Elements fields = e.getChildElements("fieldlabel");
        		for (int j = 0; j < fields.size(); j++) {
        			Element field = fields.get(j);
        			if (field.getAttributeValue("value").equals("Company")) {
        				l.setCompanyName(field.getValue());
        			}
        			
        			if (field.getAttributeValue("value").equals("Street")) {
        				l.setStreet(field.getValue());
        			}
        			
        			if (field.getAttributeValue("value").equals("City")) {
        				l.setCity(field.getValue());
        			}
        			
        			if (field.getAttributeValue("value").equals("Country")) {
        				l.setCountry(field.getValue());
        			}
        		}        		
        	}
        }
        
        return leads; 
	}

	private synchronized String getIAmTicket(String serviceName, String userName, String password) {
		if (cache.containsKey("iAmTicket"))
			return (String) cache.get("iAmTicket");
		
        String strTicket = null;
	    try {
            String iamUrl = "https://accounts.zoho.com/login?servicename=" + serviceName +"&FROM_AGENT=true&LOGIN_ID=" + userName + "&PASSWORD=" + password;
            URL u = new URL(iamUrl);

            HttpURLConnection c = (HttpURLConnection)u.openConnection();
            InputStream in = c.getInputStream();
            InputStreamReader ir = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(ir);

            String strLine  = null;

            while ((strLine = br.readLine()) != null) {
                if(strLine != null && strLine.startsWith("TICKET")) {
                    strTicket = strLine.substring(7);
                }
            }

            in.close();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }

        cache.put("iAmTicket", strTicket);
        return strTicket;
	}
}
