package org.example;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        long lifetimeInMilliseconds = 0;
        String filePath = "";

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader("src/main/resources/config.json"));
            JSONObject jsonObject = (JSONObject) obj;
            lifetimeInMilliseconds = (Long) jsonObject.get("lifetimeInMilliseconds");
            filePath = (String) jsonObject.get("filePath");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        Set<User> users = loadUsersFromFile(filePath);

        Scanner in = new Scanner(System.in);

        System.out.println("Для удобства тестирования: " + UUID.randomUUID());

        while (true) {
            System.out.println("Введите GUID или короткую ссылку. Для выхода exit:");

            String input = in.nextLine();

            if (input.equals("exit")) break;

            if (input.length() == 6) {
                System.out.println("Попытка переход по ссылке https://clck.ru/" + input);

                try {
                    clickShortLink(users, input, lifetimeInMilliseconds);
                } catch (IOException | URISyntaxException e) {
                    System.out.println("В ссылке https://clck.ru/" + input + " некорректный URL.");
                }

                continue;
            }

            UUID userId = null;

            try {
                userId = UUID.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Введён некорректный GUID.");
                continue;
            }

            User user = null;

            for (User currentUser : users) {
                if (currentUser.getId().equals(userId)) {
                    user = currentUser;
                    break;
                }
            }

            if (user == null) {
                User newUser = new User(userId);
                users.add(newUser);

                System.out.println("Введите URL и лимит переходов через пробел:");
                input = in.nextLine();
                String[] parts = input.split(" ");
                String link = parts[0];
                int limit = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1;
                newUser.createLink(link, limit);
            } else {
                System.out.println("Список ваших ссылок:");
                printUserLinks(user);
                String userManual = """
                        Для добавления новой ссылки введите URL и лимит переходов через пробел.
                        Для изменения лимита введите короткую ссылку и лимит переходов через пробел.
                        Для удаления ссылки введите её саму.
                        Для выхода из режима редактирования нажмите Enter.""";
                System.out.println(userManual);
                input = in.nextLine();
                String[] parts = input.split(" ");

                try {
                    String responce = "";

                    if (parts.length == 1 && parts[0].length() == 6) {
                        boolean isDeleted = user.deleteLink(parts[0]);
                        responce = (isDeleted) ? "Ссылка удалена." : "Ссылка не существует.";
                    } else if (parts.length == 2 && parts[0].length() == 6) {
                        boolean isUpdated = user.updateLink(parts[0], Integer.parseInt(parts[1]));
                        responce = (isUpdated) ? "Лимит обновлён." : "Ссылка не существует.";
                    } else if (parts.length == 2) {
                        user.createLink(parts[0], Integer.parseInt(parts[1]));
                        responce = "Ссылка добавлена";
                    }

                    System.out.println(responce);
                } catch (Exception e) {
                    continue;
                }
            }
        }

        saveUsersToFile(users, filePath);
    }

    private static void saveUsersToFile(Set<User> users, String filePath) {
        JSONArray userList = new JSONArray();

        for (User user : users) {
            JSONObject userDetails = new JSONObject();
            userDetails.put("id", user.getId().toString());
            JSONArray linksArray = new JSONArray();

            for (Link link : user.getLinks()) {
                if (link.isAlive()) {
                    JSONObject linkDetails = new JSONObject();
                    linkDetails.put("link", link.getLink());
                    linkDetails.put("shortLink", link.getShortLink());
                    linkDetails.put("limit", link.getLimit());
                    linkDetails.put("creationDate", link.getCreationDate().toInstant().toString());
                    linkDetails.put("clicksCount", link.getClicksCount());
                    linkDetails.put("isAlive", link.isAlive());
                    linksArray.add(linkDetails);
                }
            }

            userDetails.put("links", linksArray);
            userList.add(userDetails);
        }

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(userList.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<User> loadUsersFromFile(String filePath) {
        Set<User> users = new HashSet<>();
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(filePath)) {
            Object obj = jsonParser.parse(reader);
            JSONArray userList = (JSONArray) obj;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            for (Object userObj : userList) {
                JSONObject userJson = (JSONObject) userObj;
                UUID userId = UUID.fromString((String) userJson.get("id"));
                User user = new User(userId);

                JSONArray linksArray = (JSONArray) userJson.get("links");

                for (Object linkObj : linksArray) {
                    JSONObject linkJson = (JSONObject) linkObj;

                    String link = (String) linkJson.get("link");
                    String shortLink = (String) linkJson.get("shortLink");
                    int limit = ((Long) linkJson.get("limit")).intValue();
                    Date creationDate = sdf.parse((String) linkJson.get("creationDate"));
                    int clicksCount = ((Long) linkJson.get("clicksCount")).intValue();
                    boolean isAlive = (Boolean) linkJson.get("isAlive");

                    user.addLink(new Link(link, shortLink, limit, creationDate, clicksCount, isAlive));
                }

                users.add(user);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

        return users;
    }

    private static void printUserLinks(User user) {
        for (Link link : user.getLinks()) {
            String row = link.getShortLink() + " " + link.getClicksCount() + "/" + link.getLimit() + " " + link.getLink();
            System.out.println(row);
        }
    }

    private static void clickShortLink(Collection<User> users, String shortLink, long lifetimeInMilliseconds)
            throws URISyntaxException, IOException {
        User currentUser = null;
        Link currentLink = null;

        for (User user : users) {
            currentUser = user;

            currentLink = user.searchLink(shortLink);

            if (currentLink == null) {
                System.out.println("Ссылка не найдена");
            } else {
                for (Link link : currentUser.getLinks()) {
                    if (link.getShortLink().equals(shortLink) && link.isAlive()) {
                        currentLink = link;
                        break;
                    }
                }

                try {
                    currentLink.click(lifetimeInMilliseconds);
                } catch (LinkException e) {
                    currentUser.Notify(e.getMessage());
                } catch (URISyntaxException e) {
                    System.out.println("В ссылке https://clck.ru/" + shortLink + " некорректный URL.");
                }
            }
        }
    }
}