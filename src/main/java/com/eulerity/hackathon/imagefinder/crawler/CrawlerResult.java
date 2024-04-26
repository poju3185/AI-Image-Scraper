package com.eulerity.hackathon.imagefinder.crawler;

public abstract class CrawlerResult {
    public enum ResultType {
        IMAGE_RESULT, LOGO_RESULT, PAGE_RESULT
    }
    protected ResultType resultType;
    public abstract boolean canRunAsynchronously();
    public abstract void useAI();
}
