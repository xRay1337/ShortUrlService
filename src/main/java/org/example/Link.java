package org.example;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Link {
    private String link;
    private String shortLink;
    private int limit;
    private Date creationDate;
    private int clicksCount;
    private boolean isAlive;

    public Link(String link, String shortLink, int limit, Date creationDate, int clicksCount, boolean isAlive) {
        this.link = link;
        this.shortLink = shortLink;
        this.limit = limit;
        this.creationDate = creationDate;
        this.clicksCount = clicksCount;
        this.isAlive = isAlive;
    }

    public Link(String link, int limit, Collection<Link> currentLinks) {
        if (limit < 1) {
            throw new IllegalArgumentException();
        }

        String shortLink = null;

        while (true) {
            shortLink = UUID.randomUUID().toString().substring(0, 6);

            if (!currentLinks.contains(shortLink)) break;
        }

        this.link = link;
        this.shortLink = shortLink;
        this.limit = limit;
        this.creationDate = new Date();
        this.isAlive = true;
    }

    public String getShortLink() {
        return this.shortLink;
    }

    public String getLink() {
        return this.link;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getClicksCount() {
        return this.clicksCount;
    }

    public Date getCreationDate() {
        return this.creationDate;
    }

    public void click(long lifetimeInMilliseconds) throws LinkException, URISyntaxException, IOException {
        long differenceInMilliseconds = new Date().getTime() - creationDate.getTime();

        if (differenceInMilliseconds >= lifetimeInMilliseconds) {
            this.isAlive = false;
            throw new LinkException("Время жизни ссылки https://clck.ru/" + shortLink + " истекло.");
        }

        clicksCount += 1;

        if (clicksCount > limit) {
            this.isAlive = false;
            throw new LinkException("Достигнут лимит переходов по ссылке https://clck.ru/" + shortLink + ".");
        }

        URI uri = new URI(link);

        Desktop.getDesktop().browse(uri);
    }

    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Link link)) return false;
        return Objects.equals(shortLink, link.shortLink);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shortLink);
    }

    @Override
    public String toString() {
        return shortLink;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

}