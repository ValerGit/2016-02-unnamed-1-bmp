package frontend.servlets;

import base.AccountService;
import base.UserService;
import com.google.gson.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

import dbservice.DatabaseException;
import main.Context;
import base.datasets.UserDataSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignUpServlet extends HttpServlet {
    public static final String PATH = "/api/user/*";
    private static final Logger LOGGER = LogManager.getLogger(SignUpServlet.class);
    private AccountService accountService;
    private UserService userService;

    public SignUpServlet(Context context) {
        this.userService = (UserService) context.get(UserService.class);
        this.accountService = (AccountService) context.get(AccountService.class);
    }

    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
        final JsonObject responseBody = new JsonObject();
        final BufferedReader bufferedReader = request.getReader();
        final JsonStreamParser jsonParser = new JsonStreamParser(bufferedReader);
        JsonElement message = new JsonObject();
        if (!message.isJsonObject()) {
            goOut(response, responseBody, HttpServletResponse.SC_BAD_REQUEST, "Not a JSON");
            response.getWriter().println(responseBody);
            return;
        }
        try {
            if (jsonParser.hasNext()) message = jsonParser.next();
        } catch (JsonParseException e) {
            goOut(response, responseBody, HttpServletResponse.SC_BAD_REQUEST, "Can\'t parse JSON");
            response.getWriter().println(responseBody);
            return;
        }

        LOGGER.info("Incoming message: {}", message.toString());
        if (message.getAsJsonObject().get("login") == null || message.getAsJsonObject().get("email") == null
                || message.getAsJsonObject().get("password") == null) {
            goOut(response, responseBody, HttpServletResponse.SC_BAD_REQUEST, "Not all params send");
            response.getWriter().println(responseBody);
            return;
        }

        final String login = message.getAsJsonObject().get("login").getAsString();
        final String email = message.getAsJsonObject().get("email").getAsString();
        final String password = message.getAsJsonObject().get("password").getAsString();
        long newUserId = 0;
        try {
            if (!userService.isEmailUnique(email)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseBody.add("error", new JsonPrimitive("Email already exist"));
                responseBody.add("field", new JsonPrimitive("email"));
                response.getWriter().println(responseBody);
                return;
            }
            if (!userService.isLoginUnique(login)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseBody.add("error", new JsonPrimitive("Login already exist"));
                responseBody.add("field", new JsonPrimitive("login"));
                response.getWriter().println(responseBody);
                return;
            }
            userService.saveUser(new UserDataSet(login, password, email));
            final String sessionId = request.getSession().getId();
            newUserId = userService.getUserByLogin(login).getId();
            accountService.addSessions(sessionId, newUserId);
        } catch (DatabaseException e) {
            goOut(response, responseBody, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            response.getWriter().println(responseBody);
            return;
        }
        responseBody.add("id", new JsonPrimitive(newUserId));
        response.setStatus(HttpServletResponse.SC_OK);
        LOGGER.info("Rigister user {}", login);
        response.getWriter().println(responseBody);
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        final JsonObject responseBody = new JsonObject();
        UserDataSet currUser = null;
        try {
            currUser = checkRequest(request);
        } catch (NumberFormatException e) {
            goOut(response, responseBody, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.getWriter().println(responseBody);
            return;
        } catch (DatabaseException e) {
            goOut(response, responseBody, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            response.getWriter().println(responseBody);
            return;
        }

        if (currUser == null) {
            goOut(response, responseBody, HttpServletResponse.SC_BAD_REQUEST, "User doesn\'t exist");
            response.getWriter().println(responseBody);
            return;
        }

        final Long currUserId = currUser.getId();
        response.setStatus(HttpServletResponse.SC_OK);
        responseBody.add("id", new JsonPrimitive(currUserId));
        responseBody.add("login", new JsonPrimitive(currUser.getLogin()));
        responseBody.add("email", new JsonPrimitive(currUser.getEmail()));
        LOGGER.info("Get info about user {}", currUser.getLogin());
        response.getWriter().println(responseBody);
    }


    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    public UserDataSet checkRequest(HttpServletRequest request) throws NumberFormatException, DatabaseException {
        if (request.getPathInfo() == null)
            throw new NumberFormatException("Wrong request");
        final String requestUserId = request.getPathInfo().replace("/", "");
        if (requestUserId == null || !isInteger(requestUserId, 10))
            throw new NumberFormatException("Wrong incoming userId");
        final long userDbId = Integer.parseInt(requestUserId);
        return userService.getUserById(userDbId);
    }

    private void goOut(HttpServletResponse response, JsonObject responseBody,
                       int status, String error) {
        response.setStatus(status);
        responseBody.add("error", new JsonPrimitive(error));
        LOGGER.error(error);
    }
}