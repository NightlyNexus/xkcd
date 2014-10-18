package com.nightlynexus.xkcd;

public class Comic {

    private final int num;
    private final String link;
    private final String news;
    private final String safeTitle;
    private final String title;
    private final String transcript;
    private final String alt;
    private final String img;
    private final String day;
    private final String month;
    private final String year;


    public Comic(int num, String link, String news, String safeTitle,
                 String title, String transcript, String alt, String img,
                 String day, String month, String year) {
        this.num = num;
        this.link = link;
        this.news = news;
        this.safeTitle = safeTitle;
        this.title = title;
        this.transcript = transcript;
        this.alt = alt;
        this.img = img;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public int getNum() {
        return num;
    }

    public String getLink() {
        return link;
    }

    public String getNews() {
        return news;
    }

    public String getSafeTitle() {
        return safeTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getAlt() {
        return alt;
    }

    public String getImg() {
        return img;
    }

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }
}
