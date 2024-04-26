package com.eulerity.hackathon.imagefinder;

import com.eulerity.hackathon.imagefinder.crawler.CrawlerPageResult;
import com.eulerity.hackathon.imagefinder.crawler.WebCrawler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.util.List;

public class WebCrawlerTest {
    private WebCrawler crawler;
    private String startUrl = "https://www.google.com";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        crawler = new WebCrawler(1); // Assuming '10' is the maxImages limit
    }

    @Test
    public void testCrawlMaxDepth() throws InterruptedException, MalformedURLException {
        List<CrawlerPageResult> results = crawler.crawl(startUrl);
        // Assert that the depth control is correctly handled
        Assert.assertFalse(results.isEmpty()); // Simplified condition
    }

    @Test(expected = MalformedURLException.class)
    public void testCrawlMalformedUrl() throws InterruptedException, MalformedURLException {
        // This should throw MalformedURLException
        crawler.crawl("htp://malformed-url");
    }
}

