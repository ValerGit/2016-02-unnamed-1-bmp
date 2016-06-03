package game;

import frontend.messages.*;
import game.messages.MessageRemoveUser;
import main.Context;
import messagesystem.Abonent;
import messagesystem.Address;
import messagesystem.MessageSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class GameSession implements Abonent {

    private static final long MAX_OFFLINE_DURATION = 5 * 60 * 1000;
    private static final long CHECK_OFFLINE_DURATION = 6 * 60 * 1000;
    private static final long MAX_GAME_DURATION = 30 * 60 * 1000;
    private static final Logger LOGGER = LogManager.getLogger(GameSession.class);

    private static final AtomicLong LATEST_ID = new AtomicLong(1);

    private static final int USERS = 2;
    private static final int STATE_NOT_STARTED = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_FINISHED = 2;

    private long startTime;
    private final GameFieldProperties gameFieldProperties;
    private final ArrayList<GameUser> gameUsers = new ArrayList<>();
    private final long id;
    private final Address address = new Address();
    private final MessageSystem messageSystem;
    private int state = STATE_NOT_STARTED;
    private int gameTurn = 0;


    public GameSession(Context context, GameFieldProperties gameFieldProperties) {
        this.messageSystem = (MessageSystem) context.get(MessageSystem.class);

        this.gameFieldProperties = gameFieldProperties;
        this.id = LATEST_ID.getAndIncrement();
        this.startTime = new Date().getTime();

        LOGGER.info("Created game session, id {}", this.id);
    }

    public boolean addUser(GameUser user) {
        LOGGER.info("Attempting to add user to game session id {}, user {}", this.id, user.getName());
        if (this.state == STATE_NOT_STARTED && this.gameUsers.size() < USERS) {
            if (user.isReadyForGame() && this.gameFieldProperties.equals(user.getFieldProperties())) {
                LOGGER.info("Added user to game session id {}, user {}", this.id, user.getName());
                gameUsers.add(user);
                return true;
            }
        }
        return false;
    }

    public long getId() {
        return this.id;
    }

    public GameFieldProperties getGameFieldProperties() {
        return this.gameFieldProperties;
    }

    public boolean isNotStarted() {
        return this.state == STATE_NOT_STARTED;
    }

    public boolean isStarted() {
        return this.state == STATE_STARTED;
    }

    public boolean isFinished() {
        return this.state == STATE_FINISHED;
    }

    public void start() {
        if (this.state == STATE_NOT_STARTED && this.gameUsers.size() == USERS) {
            LOGGER.info("Start game session id {}", this.id);
            this.state = STATE_STARTED;
            this.startTime = new Date().getTime();

            this.gameTurn = (int) Math.round(Math.random()) % USERS;
            if(this.getCurrentTurnUser().isBot()) {
                this.gameTurn = (this.gameTurn + 1) % USERS;
            }
            this.notifyStart();
            this.notifyOpponentOnline();
            this.repeatTurn();
        }
    }

    public void finish() {
        if (this.state != STATE_FINISHED) {
            LOGGER.info("Finish game session id {}", this.id);
            this.state = STATE_FINISHED;
            for (GameUser user : gameUsers) {
                if(user.getUser() == null) continue;
                messageSystem.sendMessage(new MessageRemoveUser(this.address,
                        messageSystem.getAddressService().getGameMechanicsAddressFor(user.getUser()),
                        user));
            }
        }
    }

    public void shoot(GameUser gameUser, int x, int y, @Nullable Consumer<GameFieldShootResult> cb) {
        if (!this.isTurnOf(gameUser) || this.state != STATE_STARTED) {
            return;
        }

        final GameFieldShipDeck deck = new GameFieldShipDeck(x, y);
        if (!deck.isValidForGameFieldProperties(this.gameFieldProperties)) {
            return;
        }

        final GameUser opponent = this.getOpponent(gameUser);

        if(opponent == null) {
            return;
        }

        final GameFieldShootResult result = opponent.getField().shoot(x, y);
        LOGGER.info("Shoot in game session id {}, user {}, x {}, y {}, result {}", this.id, gameUser.getName(), x, y, result.getState());

        if (cb != null) {
            cb.accept(result);
        }

        messageSystem.sendMessage(new MessageNotifyShootResult(this.address,
                messageSystem.getAddressService().getWebSocketServiceAddress(),
                gameUser, result, false));
        messageSystem.sendMessage(new MessageNotifyShootResult(this.address,
                messageSystem.getAddressService().getWebSocketServiceAddress(),
                opponent, result, true));

        if (opponent.getField().isKilled()) {
            LOGGER.info("Kill opponent {} in game session id {}", opponent.getName(), this.id);
            this.win(gameUser);
        } else {
            if (result.isMiss()) {
                this.nextTurn();
            } else {
                this.repeatTurn();
            }
        }
    }

    public void giveUp(GameUser gameUser) {
        if (this.state == STATE_NOT_STARTED) {
            messageSystem.sendMessage(new MessageNotifyGameOver(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    gameUser, false));
            this.finish();
            return;
        }
        LOGGER.info("Give up in game session id {}, user {}", this.id, gameUser.getName());

        final GameUser opponent = this.getOpponent(gameUser);

        if(opponent != null) {
            this.win(opponent);
        }
    }

    public void win(GameUser gameUser) {
        if (this.state == STATE_STARTED) {
            LOGGER.info("Win in game session id {}, user {}", this.id, gameUser.getName());
            final GameUser opponent = this.getOpponent(gameUser);
            gameUser.incScore();
            messageSystem.sendMessage(new MessageNotifyGameOver(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    gameUser, true));
            if(opponent != null) {
                if(opponent.isBot()) {
                    final Long uid = gameUser.getUser() == null ? 0 : gameUser.getUser().getId();
                    LOGGER.info("USER WON BOT: user name {}, user id {}", gameUser.getName(), uid);
                }
                messageSystem.sendMessage(new MessageNotifyGameOver(this.address,
                        messageSystem.getAddressService().getWebSocketServiceAddress(),
                        opponent, false));
            }
            this.finish();
        }
    }

    public void tooLong() {
        if (this.state == STATE_STARTED) {
            LOGGER.info("Session is too long game session id {}", this.id);
            messageSystem.sendMessage(new MessageNotifyTooLong(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getCurrentTurnUser()));
            messageSystem.sendMessage(new MessageNotifyTooLong(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getNextTurnUser()));

            messageSystem.sendMessage(new MessageNotifyGameOver(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getCurrentTurnUser(), false));
            messageSystem.sendMessage(new MessageNotifyGameOver(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getNextTurnUser(), false));

            this.finish();
        }
    }

    public long getDuration() {
        return new Date().getTime() - this.startTime;
    }

    public boolean isTurnOf(GameUser user) {
        final int key = this.gameUsers.indexOf(user);
        return key > -1 && this.gameTurn == key;
    }

    public GameUser getCurrentTurnUser() {
        return this.gameUsers.get(this.gameTurn);
    }

    public GameUser getNextTurnUser() {
        return this.gameUsers.get((this.gameTurn + 1) % USERS);
    }

    public void nextTurn() {
        this.gameTurn = (this.gameTurn + 1) % USERS;
        this.repeatTurn();
    }

    public void repeatTurn() {
        LOGGER.info("Turn in game session id {}, user {}", this.id, this.getCurrentTurnUser().getName());

        this.notifyTurn();

        if (this.getCurrentTurnUser().isBot() && this.getCurrentTurnUser().getBotHelper() != null) {
            this.getCurrentTurnUser().getBotHelper().shoot();
        }
    }

    public void checkOffline() {
        if (this.state == STATE_STARTED && this.getDuration() > CHECK_OFFLINE_DURATION) {
            if (this.getCurrentTurnUser().getOfflineDuration() > MAX_OFFLINE_DURATION) {
                LOGGER.info("Offline in game session id {}, user {}", this.id, this.getCurrentTurnUser().getName());
                this.win(this.getNextTurnUser());
            } else if (this.getNextTurnUser().getOfflineDuration() > MAX_OFFLINE_DURATION) {
                LOGGER.info("Offline in game session id {}, user {}", this.id, this.getNextTurnUser().getName());
                this.win(this.getCurrentTurnUser());
            }
        }
    }

    public void checkTooLong() {
        if (this.state == STATE_STARTED && this.getDuration() > MAX_GAME_DURATION) {
            this.tooLong();
        }
    }

    @Nullable
    public GameUser getOpponent(GameUser user) {
        final int key = this.gameUsers.indexOf(user);
        if (gameUsers.size() > 1) return this.gameUsers.get((key + 1) % USERS);
        return null;
    }

    public void notifyStart() {
        if (this.state == STATE_STARTED) {
            LOGGER.info("Notify start in game session id {}", this.id);
            messageSystem.sendMessage(new MessageNotifyStart(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getCurrentTurnUser(), this.getNextTurnUser()));
            messageSystem.sendMessage(new MessageNotifyStart(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getNextTurnUser(), this.getCurrentTurnUser()));
        }
    }

    public void notifyTurn() {
        if (this.state == STATE_STARTED) {
            LOGGER.info("Notify turn in game session id {}", this.id);
            messageSystem.sendMessage(new MessageNotifyTurn(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getCurrentTurnUser(), true));
            messageSystem.sendMessage(new MessageNotifyTurn(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getNextTurnUser(), false));
        }
    }

    public void notifyOpponentOnline() {
        if (this.state == STATE_STARTED) {
            LOGGER.info("Notify online in game session id {}", this.id);
            messageSystem.sendMessage(new MessageOpponentOnline(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getCurrentTurnUser(), this.getNextTurnUser()));
            messageSystem.sendMessage(new MessageOpponentOnline(this.address,
                    messageSystem.getAddressService().getWebSocketServiceAddress(),
                    this.getNextTurnUser(), this.getCurrentTurnUser()));
        }
    }

    @Override
    public Address getAddress() {
        return address;
    }
}
