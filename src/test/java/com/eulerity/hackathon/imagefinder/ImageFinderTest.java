package com.eulerity.hackathon.imagefinder;


import java.io.*;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.mockito.Mockito;

import com.eulerity.hackathon.imagefinder.ImageFinder;
import com.google.gson.Gson;

public class ImageFinderTest {

    public HttpServletRequest request;
    public HttpServletResponse response;
    public StringWriter sw;
    public HttpSession session;

    @Before
    public void setUp() throws Exception {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);

        sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Mockito.when(response.getWriter()).thenReturn(pw);
        Mockito.when(request.getRequestURI()).thenReturn("/foo/foo/foo");
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/foo/foo/foo"));
        session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(session);
    }

    @Test
    public void testFindImagesReturnsHttpOkForValidInputs() throws IOException {
        Mockito.when(request.getServletPath()).thenReturn("/main");
        Mockito.when(request.getParameter("url")).thenReturn("https://www.google.com");
        Mockito.when(request.getParameter("maxImages")).thenReturn("1");

        new ImageFinder().doPost(request, response);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void testFindImagesWithAIEnabledReturnsHttpOk() throws IOException {
        Mockito.when(request.getServletPath()).thenReturn("/main");
        Mockito.when(request.getParameter("url")).thenReturn("https://www.google.com");
        Mockito.when(request.getParameter("maxImages")).thenReturn("2");
        Mockito.when(request.getParameter("useAI")).thenReturn("true");

        new ImageFinder().doPost(request, response);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
    }


    @Test
    public void testAnnotateImageReturnsHttpOk() throws IOException {
        try (ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class)) {
            Mockito.when(request.getServletPath()).thenReturn("/annotate");
            Mockito.when(response.getOutputStream()).thenReturn(outputStream);
            String jsonInput = "{\n" +
                    "    \"url\": \"https://i.guim.co.uk/img/media/26392d05302e02f7bf4eb143bb84c8097d09144b/446_167_3683_2210/master/3683.jpg?width=465&dpr=1&s=none\",\n" +
                    "    \"detections\": [\n" +
                    "        {\n" +
                    "            \"bbox\": {\n" +
                    "                \"x\": 79.98257339000702,\n" +
                    "                \"y\": 53.262973594665525,\n" +
                    "                \"width\": 288.00329518318176,\n" +
                    "                \"height\": 224.30535593032835\n" +
                    "            },\n" +
                    "            \"score\": 0.93906987,\n" +
                    "            \"className\": \"cat\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
            Mockito.when(request.getReader()).thenReturn(reader);

            new ImageFinder().doPost(request, response);

            Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        }


    }

}

//        "bbox":{
//        "x":79.98257339000702,
//        "y":53.262973594665525,
//        "width":288.00329518318176,
//        "height":224.30535593032835
//        },
//        "score":0.93906987,
//        "className":"cat"
//        }


