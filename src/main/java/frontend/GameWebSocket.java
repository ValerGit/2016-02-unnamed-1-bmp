package frontend;


import base.*;
import base.datasets.UserDataSet;
import com.google.gson.*;
import com.sun.istack.internal.NotNull;
import frontend.messages.MessageAddSocket;
import frontend.messages.MessageOpponentOnline;
import frontend.messages.MessageRemoveSocket;
import game.*;
import game.messages.MessageAddUserForBotGame;
import game.messages.MessageAddUserForFriendGame;
import game.messages.MessageAddUserForRandomGame;
import game.messages.MessageRemoveUser;
import main.Context;
import messagesystem.Abonent;
import messagesystem.Address;
import messagesystem.MessageSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;


@WebSocket
public class GameWebSocket implements Abonent {
    private Address address = new Address();
    private MessageSystem messageSystem;
    private Session session;
    private final UserDataSet user;
    private final Context context;
    //    private final WebSocketService webSocketService;
//    private final GameMechanics gameMechanics;
    private final UserService userService;
    private GameSession gameSession;
    private GameUser gameUser;

    private static final Logger LOGGER = LogManager.getLogger(GameWebSocket.class);

    GameWebSocket(@NotNull UserDataSet user, Context context) {
        this.user = user;
        this.context = context;
//        this.webSocketService = (WebSocketService) context.get(WebSocketService.class);
//        this.gameMechanics = (GameMechanics) context.get(GameMechanics.class);
        this.messageSystem = (MessageSystem) context.get(MessageSystem.class);
        this.userService = (UserService) context.get(UserService.class);
    }

    public UserDataSet getUser() {
        return this.user;
    }

    @OnWebSocketConnect
    public void onOpen(Session ses) {
        this.session = ses;
        LOGGER.info("Opened web socket, user id {}", this.user.getId());
//        this.webSocketService.addSocket(this);
        messageSystem.sendMessage(new MessageAddSocket(this.address,
                messageSystem.getAddressService().getWebSocketServiceAddress(), this));

//        final GameSession gameSession = this.gameMechanics.getUserGameSession(this.user);
//        final GameUser gameUser = this.gameMechanics.getGameUser(this.user);

        if (gameUser != null && gameSession != null) {
            gameUser.setOnline();
            final GameUser opponent = gameSession.getOpponent(gameUser);
            if (opponent != null) {
//                this.webSocketService.notifyOpponentOnline(opponent, gameUser);
                messageSystem.sendMessage(new MessageOpponentOnline(this.address,
                        messageSystem.getAddressService().getWebSocketServiceAddress(),
                        opponent, gameUser));

            }
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOGGER.info("Closed web socket, user id {} by {}", this.user.getId(), reason);
//        this.webSocketService.removeSocket(this);
        messageSystem.sendMessage(new MessageRemoveSocket(this.address,
                messageSystem.getAddressService().getWebSocketServiceAddress(), this));

//        final GameSession gameSession = this.gameMechanics.getUserGameSession(this.user);
//        final GameUser gameUser = this.gameMechanics.getGameUser(this.user);

        if (gameUser != null && gameSession != null) {
            gameUser.setOffline();
            final GameUser opponent = gameSession.getOpponent(gameUser);
            if (opponent != null) {
//                this.webSocketService.notifyOpponentOnline(opponent, gameUser);
                messageSystem.sendMessage(new MessageOpponentOnline(this.address,
                        messageSystem.getAddressService().getWebSocketServiceAddress(),
                        opponent, gameUser));
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(String data) {
        LOGGER.info("Message in socket, user id {}: \"{}\"", this.user.getId(), data);

        try {
            final JsonElement jsonElement = new JsonParser().parse(data);
            final String action = jsonElement.getAsJsonObject().getAsJsonPrimitive("action").getAsString();
            if (action == null) {
                throw new JsonSyntaxException("No action field in web socket message");
            }
            LOGGER.info("Message in web socket, user id {}: action is {}", this.user.getId(), action);

            final JsonObject message = jsonElement.getAsJsonObject();

            switch (action) {
                case "getGameStatus":
                    this.onMessageGetGameStatus(message);
                    break;
                case "initNewGame":
                    this.onMessageInitNewGame(message);
                    break;
                case "giveUp":
                    this.onMessageGiveUp(message);
                    break;
                case "shoot":
                    this.onMessageShoot(message);
                    break;
                default:
                    throw new JsonSyntaxException("Unknown action");
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error(e.getMessage());
            this.send(new GameWebSocketMessage(GameWebSocketMessage.MessageType.ERROR, "Wrong JSON: " + e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage());
            this.send(new GameWebSocketMessage(GameWebSocketMessage.MessageType.ERROR, "Unexpected error"));
        }
    }

    public void onMessageGetGameStatus(JsonObject message) {
        final GameWebSocketMessage result = new GameWebSocketMessage(GameWebSocketMessage.MessageType.GAME_STATUS);

//        final boolean hasGame = this.gameMechanics.hasUserGameSession(this.user);
//        result.setOk(hasGame);
//
//        if (!hasGame) {
//            this.send(result);
//            return;
//        }
//        final GameSession gameSession = this.gameMechanics.getUserGameSession(this.user);
//        final GameUser gameUser = this.gameMechanics.getGameUser(this.user);
        if (gameSession == null) {
            result.setOk(false);
            this.send(result.getAsJSON());
            return;
        }
        result.setId(gameSession.getId());

        final JsonObject resultJson = result.getAsJSON();
        resultJson.add("started", new JsonPrimitive(gameSession.isStarted()));

        final JsonArray shipsJson = new JsonArray();
        gameUser.getField().getShips().forEach(ship -> {
            final JsonArray shipJson = new JsonArray();
            shipJson.add(ship.getX());
            shipJson.add(ship.getY());
            shipJson.add(ship.getLength());
            shipJson.add(ship.isVertical());
            shipsJson.add(shipJson);
        });

        resultJson.add("ships", shipsJson);

        final JsonArray shootsJson = new JsonArray();
        gameUser.getField().getShoots().forEach(shoot -> {
            final JsonArray shootJson = new JsonArray();
            shootJson.add(shoot.getX());
            shootJson.add(shoot.getY());
            shootJson.add(!shoot.isMiss());
            shootsJson.add(shootJson);
        });
        resultJson.add("shoots", shootsJson);

        if (gameSession.isStarted()) {
            final GameUser opponent = gameSession.getOpponent(gameUser);
            if (opponent != null) {
                gameSession.notifyOpponentOnline();
                final String opponentName = opponent.getName();
                resultJson.add("opponentName", new JsonPrimitive(opponentName));
                final JsonArray opponentShipsJson = new JsonArray();
                opponent.getField().getShips().stream().filter(GameFieldShip::isKilled).forEach(ship -> {
                    final JsonArray shipJson = new JsonArray();
                    shipJson.add(ship.getX());
                    shipJson.add(ship.getY());
                    shipJson.add(ship.getLength());
                    shipJson.add(ship.isVertical());
                    opponentShipsJson.add(shipJson);
                });
                resultJson.add("opponentShips", opponentShipsJson);

                final JsonArray opponentShootsJson = new JsonArray();
                opponent.getField().getShoots().forEach(shoot -> {
                    final JsonArray shootJson = new JsonArray();
                    shootJson.add(shoot.getX());
                    shootJson.add(shoot.getY());
                    shootJson.add(!shoot.isMiss());
                    opponentShootsJson.add(shootJson);
                });
                resultJson.add("opponentShoots", opponentShootsJson);
            }
        }

        this.send(resultJson);
    }

    public void onMessageInitNewGame(JsonObject message) {
        final String gameMode = message.getAsJsonPrimitive("mode").getAsString();
        final GameFieldProperties gameFieldProperties = GameFieldProperties.getProperties();

        if (gameFieldProperties == null) {
            this.send(new GameWebSocketMessage(GameWebSocketMessage.MessageType.ERROR, "Unexpected error"));
            return;
        }

        final GameField gameField = new GameField(gameFieldProperties);

        message.getAsJsonArray("ships").forEach((ship) -> {
            final int x = ship.getAsJsonArray().get(0).getAsInt();
            final int y = ship.getAsJsonArray().get(1).getAsInt();
            final int length = ship.getAsJsonArray().get(2).getAsInt();
            final boolean isVertical = ship.getAsJsonArray().get(3).getAsBoolean();

            final GameFieldShip shipObj = new GameFieldShip(x, y, length, isVertical);

            gameField.addShip(shipObj);
        });

        gameUser = new GameUser(this.user, gameField, userService);
        gameSession = new GameSession(this.context, gameUser.getFieldProperties());

        if (!gameField.isValid()) {
            this.send(new GameWebSocketMessage(GameWebSocketMessage.MessageType.ERROR, "Game field is invalid"));
            return;
        }

        switch (gameMode) {
            case "bot":
//                this.gameMechanics.addUserForBotGame(gameUser, gameSession);
                messageSystem.sendMessage(new MessageAddUserForBotGame(this.address,
                        messageSystem.getAddressService().getGameMechanicsAddressFor(gameUser.getName()),
                        gameUser, gameSession));
                break;
            case "friend":
                final JsonElement gameSessionIdElement = message.getAsJsonPrimitive("id");
                final Long gameSessionId = gameSessionIdElement == null ? null : gameSessionIdElement.getAsLong();
//                this.gameMechanics.addUserForFriendGame(gameUser, gameSessionId);
                messageSystem.sendMessage(new MessageAddUserForFriendGame(this.address,
                        messageSystem.getAddressService().getGameMechanicsAddressFor(gameUser.getName()),
                        gameUser, gameSessionId));
                break;
            case "random":
            default:
//                this.gameMechanics.addUserForRandomGame(gameUser);
                messageSystem.sendMessage(new MessageAddUserForRandomGame(this.address,
                        messageSystem.getAddressService().getGameMechanicsAddressFor(gameUser.getName()),
                        gameUser, gameSession));
                break;
        }
    }

    public void onMessageGiveUp(JsonObject message) {
//        final GameSession gameSession = this.gameMechanics.getUserGameSession(this.user);
        if (gameSession != null && gameUser != null) {
//            final GameUser gameUser = this.gameMechanics.getGameUser(this.user);
            gameSession.giveUp(gameUser);
        }
    }

    public void onMessageShoot(JsonObject message) {
        final int x = message.getAsJsonPrimitive("x").getAsInt();
        final int y = message.getAsJsonPrimitive("y").getAsInt();
//        final GameSession gameSession = this.gameMechanics.getUserGameSession(this.user);
        if (gameSession != null && gameUser != null) {
//            final GameUser gameUser = this.gameMechanics.getGameUser(this.user);
            gameSession.shoot(gameUser, x, y, null);
        }
    }

    public boolean send(String message) {
        try {
            LOGGER.info("Attempting to send a message in web socket, user id {}, message: \"{}\"",
                    this.user.getId(), message);
            this.session.getRemote().sendString(message);
            return true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    public boolean send(JsonObject message) {
        return this.send(message.toString());
    }

    public boolean send(GameWebSocketMessage message) {
        return this.send(message.toString());
    }

    public void close() {
        LOGGER.info("Attempting to close web socket, user id {}", this.user.getId());
        if (session.isOpen()) {
            this.session.close();
        }
    }

    public boolean isOpen() {
        return this.session.isOpen();
    }

    @Override
    public Address getAddress() {
        return address;
    }
}

