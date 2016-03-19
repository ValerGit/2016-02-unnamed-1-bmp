package frontend.servlets;

import base.AccountService;
import base.DBService;
import com.google.gson.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.Context;
import base.datasets.UserDataSet;

public class SignInServlet extends HttpServlet {
    public static final String PATH = "/api/session";
    private static final Logger LOGGER = LogManager.getLogger();
    private AccountService accountService;
    private DBService dbService;

    public SignInServlet(Context context) {
        this.dbService = (DBService) context.get(DBService.class);
        this.accountService = (AccountService) context.get(AccountService.class);
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        final JsonObject responseBody = new JsonObject();

        final String sessionId = request.getSession().getId();
        final Long userId = accountService.getUserIdBySesssion(sessionId);

        if (userId != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            responseBody.add("id", new JsonPrimitive(userId));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseBody.add("error", new JsonPrimitive("User not authorized"));
        }
        response.getWriter().println(responseBody);
    }

    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
        final JsonObject responseBody = new JsonObject();
        final BufferedReader bufferedReader = request.getReader();
        final JsonStreamParser jsonParser = new JsonStreamParser(bufferedReader);

        try {
            JsonElement message = new JsonObject();
            if (jsonParser.hasNext()) {
                message = jsonParser.next();
            }

            LOGGER.info("Incoming message: {}", message.toString());

            if (message.getAsJsonObject().get("login") == null
                    || message.getAsJsonObject().get("password") == null) {
                throw new Exception("Not all params send");
            }

            final String login = message.getAsJsonObject().get("login").getAsString();
            final String password = message.getAsJsonObject().get("password").getAsString();

            final UserDataSet user = dbService.getUserByLogin(login);
            if (user == null || !password.equals(user.getPassword())) {
                throw new Exception("Wrong email or password");
            }

            final String sessionId = request.getSession().getId();
            accountService.addSessions(sessionId, user.getId());

            response.setStatus(HttpServletResponse.SC_OK);
            responseBody.add("id", new JsonPrimitive(user.getId()));

        } catch (JsonParseException e) {
            LOGGER.error(e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseBody.add("error", new JsonPrimitive("wrong json"));
        } catch (Exception e) {
            LOGGER.error(e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseBody.add("error", new JsonPrimitive(e.getMessage()));
        }

        response.getWriter().println(responseBody);
    }

    @Override
    public void doDelete(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final JsonObject responseBody = new JsonObject();
        final String sessionId = request.getSession().getId();

        if (!accountService.logout(sessionId)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseBody.add("error", new JsonPrimitive("This session is not registered"));
            LOGGER.error("This session is not registered");

        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        response.getWriter().println(responseBody);
    }
}
