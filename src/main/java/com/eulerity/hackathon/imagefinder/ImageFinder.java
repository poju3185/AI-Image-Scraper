package com.eulerity.hackathon.imagefinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eulerity.hackathon.imagefinder.crawler.CrawlerPageResult;
import com.eulerity.hackathon.imagefinder.crawler.WebCrawler;
import com.eulerity.hackathon.imagefinder.objectDetector.AnnotateRequest;
import com.eulerity.hackathon.imagefinder.objectDetector.ImageAnnotator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.opencv.core.Mat;

@WebServlet(name = "ImageFinder", urlPatterns = {"/main", "/annotate"})
public class ImageFinder extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected static final Gson GSON = new GsonBuilder().create();

    //This is just a test array
    public static final String[] testImages = {"https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg?auto=compress&format=tiny", "https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg?auto=compress&format=tiny", "https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg?auto=compress&format=tiny", "https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg?auto=compress&format=tiny"};

    static {
        nu.pattern.OpenCV.loadShared();
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        switch (path) {
            case "/main":
                findImages(req, resp);
                break;
            case "/annotate":
                annotateImage(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found");
                break;
        }
    }

    private static void findImages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Processing params
        resp.setContentType("text/html");
        String url = req.getParameter("url");
        String maxImagesParam = req.getParameter("maxImages");
        String useAIParam = req.getParameter("useAI");
        int maxImages;
        try {
            maxImages = Integer.parseInt(maxImagesParam);
        } catch (NumberFormatException e) {
            maxImages = 10;
        }
        maxImages = Math.min(maxImages, 100);
        boolean useAI = useAIParam != null && useAIParam.equals("true");

        if (url != null && !url.isEmpty()) {
            try {
                // Create a new WebCrawler instance with a specified maximum number of images.
                WebCrawler crawler = new WebCrawler(maxImages);
                // Crawl the provided URL and retrieve page results containing image URLs.
                List<CrawlerPageResult> results = crawler.crawl(url);
                // If useAI flag is true, apply AI processing to each result.
                if (useAI) {
                    for (CrawlerPageResult result : results) {
                        result.useAI();
                    }
                }
                // Convert the results to JSON format for response output.
                String jsonResponse = GSON.toJson(results);
                resp.setStatus(HttpServletResponse.SC_OK);
                // Send the JSON response back to the client.
                resp.getWriter().print(jsonResponse);

            } catch (MalformedURLException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("URL is malformed.");
            } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().print("Something went wrong, please try again later.");
            }
        } else {
            // Set client error status and send an error message if the URL parameter is missing.
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("URL parameter is missing or empty.");
        }

    }

    private static void annotateImage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            BufferedReader reader = req.getReader();
            AnnotateRequest annotateRequest = GSON.fromJson(reader, AnnotateRequest.class);
            if (annotateRequest.getUrl() == null) {
                throw new IllegalArgumentException("Missing required fields: url.");
            }

            // Validate the presence of the required URL field in the request.
            Mat image = ImageAnnotator.urlToImage(annotateRequest.getUrl());
            if (image.empty()) {
                throw new IllegalArgumentException("Image url could not be loaded.");
            }
            Mat annotatedImage = ImageAnnotator.annotateImage(image, annotateRequest.getDetections());
            byte[] imageData = ImageAnnotator.matToBytes(annotatedImage);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("image/jpeg");
            resp.setContentLength(imageData.length);
            ServletOutputStream outStream = resp.getOutputStream();
            outStream.write(imageData);
            outStream.close();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().print("Invalid Argument: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter().print("Something went wrong: " + e.getMessage());
        }
    }

}
