package com.eulerity.hackathon.imagefinder.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler {
    private static final int MAX_DEPTH = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Set<String> collectImages = ConcurrentHashMap.newKeySet();
    private final int maxImages;
    private String startUrl;
    private String originHost;

    public WebCrawler(int maxImages) {
        this.maxImages = maxImages;
    }

    /**
     * Performs a BFS crawl starting from the specified URL.
     * This method processes web pages up to a defined depth, collecting logos and images.
     * Each layer of the BFS is processed in parallel using multi-threading.
     *
     * @param startUrl the starting URL for the crawl
     * @return a list of CrawlerPageResult containing the results of the crawl
     * @throws InterruptedException  if the thread execution is interrupted
     * @throws MalformedURLException if the provided URL is not properly formatted
     */
    public List<CrawlerPageResult> crawl(String startUrl) throws InterruptedException, MalformedURLException {
        this.startUrl = startUrl;
        this.originHost = new URL(startUrl).getHost();
        // Initialize a queue to manage URLs for BFS
        final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
        urlQueue.add(startUrl);
        List<CrawlerPageResult> results = new ArrayList<>();

        // Loop through each depth level up to MAX_DEPTH and until maximum images are collected
        for (int depth = 0; depth <= MAX_DEPTH && collectImages.size() < maxImages && !urlQueue.isEmpty(); depth++) {
            System.out.println("Crawling depth " + depth + " with " + urlQueue.size() + " urls");  // Log the current depth and URL count

            int queueSize = urlQueue.size();
            CountDownLatch latch = new CountDownLatch(queueSize);  // Latch to wait for all threads to complete in this layer

            // Process each URL in the current layer
            for (int i = 0; i < queueSize; i++) {
                String url = urlQueue.poll();
                executor.submit(() -> {
                    try {
                        List<CrawlerResult> imagesFound = processPage(url, urlQueue);  // Process the page and collect results
                        if (imagesFound != null && !imagesFound.isEmpty()) {
                            synchronized (results) {
                                results.add(new CrawlerPageResult(url, imagesFound));  // Store page results if images were found
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(); // Wait for the entire layer to process, so we can accommodate both BFS and multi-threading
        }

        executor.shutdown();
        // Wait for remaining tasks to finish
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        return results;
    }

    /**
     * Processes a single page, and enqueues valid URLs for further BFS crawling.
     * This method is part of a BFS where each visited page can add new URLs to the queue.
     * It incorporates random sleep intervals to avoid triggering anti-scraping mechanisms.
     *
     * @param url the URL of the page to process
     * @param urlQueue the queue of URLs to be processed in future BFS layers
     * @return a list of CrawlerResult objects containing the images found on the page
     */
    private List<CrawlerResult> processPage(String url, Queue<String> urlQueue) {
        // Images crawled on this page
        List<CrawlerResult> imageResults = new ArrayList<>();
        if (collectImages.size() < maxImages && visited.add(url)) {
            System.out.println("visiting: " + url);
            try {
                // Random sleep to mimic human browsing and avoid being blocked by the server
                int sleepTime = ThreadLocalRandom.current().nextInt(1000, 5001);
                System.out.println("Wait for " + sleepTime + " ms...");
                Thread.sleep(sleepTime);

                Document document = Jsoup.connect(url).get();

                try {
                    // Crawl for images on the page and add results to imageResults list
                    crawlImages(url.equals(startUrl), true, true, document, imageResults);
                } catch (MaxImagesReachedException e) {
                    // If max images limit is reached during crawl, return the results gathered so far
                    return imageResults;
                }

                // Select all hyperlink elements and enqueue valid links for further crawling
                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String nextLink = link.absUrl("href");
                    if (isValidLink(nextLink)) {
                        urlQueue.offer(nextLink);  // BFS
                    }
                }
            } catch (Exception e) {
                System.out.println("Error retrieving " + url + ": " + e.getMessage());
            }
        }
        return imageResults;  // Return the list of image results from this page
    }

    /**
     * Crawls a given document for images based on specified criteria.
     *
     * @param fetchFavicon Whether to fetch favicons from the document.
     * @param fetchLogo    Whether to fetch logo images from the document.
     * @param fetchImages  Whether to fetch general images from the document.
     * @param document     The document to crawl.
     * @param imageResults The list where extracted CrawlerResults are stored.
     * @throws MaxImagesReachedException if the maximum number of images to be collected is reached.
     */
    private void crawlImages(boolean fetchFavicon, boolean fetchLogo, boolean fetchImages, Document document, List<CrawlerResult> imageResults) throws MaxImagesReachedException {
        if (fetchFavicon) getFavicon(document, imageResults);
        if (fetchLogo) getLogoImages(document, imageResults);
        if (fetchImages) getImages(document, imageResults);
    }

    /**
     * Extracts logo images from the provided document and adds them to the specified list of image results.
     *
     * @param document     The document from which logo images are to be extracted.
     * @param imageResults The list to which extracted logo images are added as CrawlerLogoResult objects.
     * @throws MaxImagesReachedException if the maximum number of images to be collected is reached.
     */
    private void getLogoImages(Document document, List<CrawlerResult> imageResults) throws MaxImagesReachedException {
        // CSS selector string to find elements that might contain logos
        String selector = "img[class*='logo'], img[id*='logo'], img[data-testid*='logo'], img[aria-label*='logo'], img[data-link-name*='logo'], " + "a[class*='logo'], a[id*='logo'], a[data-testid*='logo'], a[aria-label*='logo'], a[data-link-name*='logo'], " + "div[class*='logo'], div[id*='logo'], div[data-testid*='logo'], div[aria-label*='logo'], div[data-link-name*='logo']";

        Elements logoElements = document.select(selector);

        for (Element logoElement : logoElements) {
            // Select SVG elements within the current logo element
            Elements svgs = logoElement.select("svg");
            for (Element svg : svgs) {
                // Check if the maximum number of images has been reached, if so, throw an exception
                if (collectImages.size() >= maxImages) throw new MaxImagesReachedException("Max image reached.");
                // Get the outer HTML of the SVG, which is the SVG element itself
                String svgXml = svg.outerHtml();
                // Add the SVG to collectImages if it's not already there and add a new CrawlerLogoResult to imageResults
                if (collectImages.add(svgXml)) {
                    imageResults.add(new CrawlerLogoResult(svgXml, true));
                }
            }

            // Select IMG elements within the current logo element
            Elements imgs = logoElement.select("img");
            for (Element img : imgs) {
                // Again, check if the maximum number of images has been reached, if so, throw an exception
                if (collectImages.size() >= maxImages) throw new MaxImagesReachedException("Max image reached.");
                // Get the absolute URL of the image, prioritizing the "src" attribute, but falling back to "data-src" if "src" is empty
                String src = img.attr("abs:src").isEmpty() ? img.attr("abs:data-src") : img.attr("abs:src");
                // Add the URL to collectImages if it's not already there, the URL is not empty, and add a new CrawlerLogoResult to imageResults
                if (collectImages.add(src) && !src.isEmpty()) {
                    imageResults.add(new CrawlerLogoResult(src, false));
                }
            }
        }
    }

    /**
     * Extracts images from the provided document and adds them to the specified list of image results.
     *
     * @param document     The document from which images are to be extracted.
     * @param imageResults The list to which extracted images are added as CrawlerImgResult objects.
     * @throws MaxImagesReachedException if the maximum number of images to be collected is reached.
     */
    private void getImages(Document document, List<CrawlerResult> imageResults) throws MaxImagesReachedException {
        Elements images = document.select("img[src], img[data-src]");

        for (Element img : images) {
            // Check if the maximum number of images has been reached, if so, throw an exception
            if (collectImages.size() >= maxImages) throw new MaxImagesReachedException("Max image reached.");
            // Get the absolute URL of the image, choosing "data-src" if "src" is empty
            String src = img.attr("abs:src").isEmpty() ? img.attr("abs:data-src") : img.attr("abs:src");
            // Add the URL to collectImages to track images collected; if successful, add new CrawlerImgResult to imageResults
            if (collectImages.add(src)) {
                imageResults.add(new CrawlerImgResult(src));
            }
        }
    }

    /**
     * Extracts favicon links from the provided document and adds them to the specified list of image results.
     *
     * @param document     The document from which favicons are to be extracted.
     * @param imageResults The list to which extracted favicons are added as CrawlerLogoResult objects.
     */
    private void getFavicon(Document document, List<CrawlerResult> imageResults) {
        // Select all link elements that define a favicon
        Elements faviconLinks = document.select("link[rel='icon'], link[rel='shortcut icon']");

        for (Element link : faviconLinks) {
            // Retrieve the absolute URL of the favicon from the "href" attribute
            String faviconUrl = link.absUrl("href");

            // If the URL is not empty, add a new CrawlerLogoResult to the imageResults list
            if (!faviconUrl.isEmpty()) {
                imageResults.add(new CrawlerLogoResult(faviconUrl, false));
            }
        }
    }

    /**
     * Determines if a given link is valid for further crawling based on domain and fragment considerations.
     *
     * @param nextLink The hyperlink being evaluated.
     * @return true if the link is on the same host as the origin host and is not an anchor link.
     * @throws MalformedURLException if the provided URLs are not properly formatted.
     */
    private boolean isValidLink(String nextLink) throws MalformedURLException {
        if (!new URL(nextLink).getHost().equals(originHost)) {
            return false;
        }

        // Filter out anchor links where there is a '#' after '/', such as '/#About'.
        int lastSlashIndex = nextLink.lastIndexOf('/');
        if (lastSlashIndex == -1) return true;
        try {
            return nextLink.charAt(lastSlashIndex + 1) != '#';
        } catch (StringIndexOutOfBoundsException e) {
            // If there is no character after '/', it's still considered a valid link
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException, MalformedURLException {
        WebCrawler crawler = new WebCrawler(10);
        List<CrawlerPageResult> results = crawler.crawl("https://www.theguardian.com/lifeandstyle/2020/sep/05/what-cats-mean-by-miaow-japans-pet-guru-knows-just-what-your-feline-friend-wants");
        System.out.println(results.size());
    }
}

class MaxImagesReachedException extends Exception {
    public MaxImagesReachedException(String message) {
        super(message);
    }
}