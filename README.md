# AI Image Scraper

## How It Works
- Enter a URL and the maximum number of images (default is 10)
- Press the submit button to initiate a crawl of the specified site and its related links to fetch the
designated number of images.
- Images fetched from each website are displayed separately. Click on the link
to view the original web page of the image.
- Logo images are highlighted with a red border.
- During the crawl of each website, there is a random delay of 1 to 5 seconds.
- Select 'Use AI Detection' to apply OCR on logos, whereas other images will
undergo object recognition.
- If objects are detected within an image, you can click on that image to see
the locations of the identified objects.
 
### Requirements
Before beginning, make sure you have the following installed and ready to use
- Maven 3.5 or higher
- Java 8
  - Exact version, **NOT** Java 9+ - the build will fail with a newer version of Java

### Setup
To start, open a terminal window and navigate to wherever you unzipped to the root directory `imagefinder`. To build the project, run the command:

>`mvn package`

>`mvn clean`

To run the project, use the following command to start the server:

>`mvn clean test package jetty:run`

Now, if you enter `localhost:8080` into your browser, you should see the `index.html` welcome page! 
