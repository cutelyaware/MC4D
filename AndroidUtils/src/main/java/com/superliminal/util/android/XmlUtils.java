package com.superliminal.util.android;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Xml;

public class XmlUtils {

    /**
     * Takes a URL and a set of desired XML keys, and returns the values for those keys in the given map.
     * Based on pattern from http://www.java2s.com/Code/Java/XML/Simplelisterextractnameandchildrentags.htm
     * 
     * @author Melinda Green - Copyright 2011 Superliminal Software
     * @throws MalformedURLException
     * @throws SAXException
     */
    public static void getNamedXMLVals(String urlStr, final Map<String, String> desiredVals) throws MalformedURLException, SAXException {
        //URL xml_url = new URL("http://api.wunderground.com/weatherstation/WXCurrentObXML.asp?ID=KCADALYC1");
        URL xml_url = new URL(urlStr);

        String xml_str = ResourceUtils.readFileFromURL(xml_url);
        org.xml.sax.ContentHandler my_handler = new DefaultHandler() {
            private String found = null;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
                if(desiredVals.containsKey(qName))
                    found = qName; // We don't have the value yet but the next call to characters() will.

            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if(found != null) {
                    // Grab the value for the most recently found key.
                    String val = new String(ch, start, length);
                    desiredVals.put(found, val); // Stash it in caller's map.
                    found = null; // Ready to find more values.
                }
                super.characters(ch, start, length);
            }

        };
        if(xml_str != null)
            Xml.parse(xml_str, my_handler);
    }
}
