package com.eulerity.hackathon.imagefinder.crawler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class CrawlerPageResult extends CrawlerResult {
    private final String pageUrl;
    private final List<CrawlerResult> crawlerResults;

    public CrawlerPageResult(String pageUrl, List<CrawlerResult> crawlerResults) {
        this.resultType = ResultType.PAGE_RESULT;
        this.pageUrl = pageUrl;
        this.crawlerResults = crawlerResults;
    }

    @Override
    public boolean canRunAsynchronously() {
        return false;
    }

    /**
     * Processes AI tasks using a mix of asynchronous and synchronous execution.
     */
    @Override
    public void useAI() {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<CrawlerResult> syncResults = new ArrayList<>();

        for (CrawlerResult crawlerResult : crawlerResults) {
            if (crawlerResult.canRunAsynchronously()) {
                executor.submit(crawlerResult::useAI);
            } else {
                syncResults.add(crawlerResult);
            }
        }
        executor.shutdown();
        for (CrawlerResult syncResult : syncResults) {
            syncResult.useAI();
        }
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}