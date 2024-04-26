package com.eulerity.hackathon.imagefinder.crawler;

import com.eulerity.hackathon.imagefinder.ocr.OCRApi;
import lombok.Getter;
import org.apache.batik.transcoder.TranscoderException;

import java.io.IOException;

@Getter
public class CrawlerLogoResult extends CrawlerResult{
    private final String imageData;
    private final boolean isSvg;
    private String ocrResult;
    public CrawlerLogoResult(String imageData, boolean isSvg) {
        this.resultType = ResultType.LOGO_RESULT;
        this.imageData = imageData;
        this.isSvg = isSvg;
    }

    @Override
    public boolean canRunAsynchronously() {
        return true;
    }

    @Override
    public void useAI(){
        useOCR();
    }
    private void useOCR() {
        try {
            if (isSvg) {
                ocrResult = OCRApi.performOCROnSvg(imageData, false);
            } else {
                ocrResult = OCRApi.performOCROnImageUrl(imageData);
            }
        } catch (IOException | TranscoderException ignored) {
        }
    }
}
