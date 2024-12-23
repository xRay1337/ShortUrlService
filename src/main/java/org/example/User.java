package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {
    private UUID id;
    private List<Link> links = new ArrayList();

    public User(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public List<Link> getLinks() {
        return links.stream().filter(Link::isAlive).toList();
    }

    public boolean addLink(Link link){
        return links.add(link);
    }

    public Link searchLink(String shortLink) {
        Link result = null;

        for (Link currentLink : links) {
            if (currentLink.isAlive() && currentLink.toString().equals(shortLink)) {
                result = currentLink;
                break;
            }
        }

        return result;
    }

    public Link createLink(String link, int limit) {
        Link newLink = new Link(link, limit, links);
        links.add(newLink);
        return newLink;
    }

    public boolean updateLink(String shortLink, int limit) {
        Link link = searchLink(shortLink);

        if (link == null) return false;
        else {
            link.setLimit(limit);
            return true;
        }
    }

    public boolean deleteLink(String shortLink) {
        Link link = searchLink(shortLink);
        return this.links.remove(link);
    }

    public void Notify(String message) {
        System.out.println("Уведомление для пользователя " + id + ": " + message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}