package game;

import base.UserService;
import base.datasets.UserDataSet;
import dbservice.DatabaseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class GameUser {

    private final String name;
    private final UserDataSet user;

    private final UserService userService;

    private final boolean isBot;
    private GameUserBotHelper botHelper;

    private final GameField field;

    private Long offlineTime = null;

    public GameUser(String name, GameField field) {
        this.name = name;
        this.user = null;
        this.userService = null;
        this.field = field;
        this.isBot = true;
    }

    public GameUser(UserDataSet user, GameField field, UserService userService) {
        this.name = null;
        this.user = user;
        this.userService = userService;
        this.field = field;
        this.isBot = false;
        this.botHelper = null;
    }

    @NotNull
    public String getName() {
        if (this.user != null && this.user.getLogin() != null) {
            return this.user.getLogin();
        }

        if (this.name != null) {
            return this.name;
        }
        return "";
    }

    @Nullable
    public UserDataSet getUser() {
        return this.user;
    }

    public void setBotHelper(@NotNull GameUserBotHelper botHelper) {
        this.botHelper = botHelper;
    }

    @Nullable
    public GameUserBotHelper getBotHelper() {
        return this.botHelper;
    }

    public boolean incScore() {
        try {
            if (this.user != null && this.userService != null) {
                this.userService.incrementUserScore(this.user.getId());
            }
            return true;
        } catch (DatabaseException e) {
            return false;
        }
    }

    public boolean isReadyForGame() {
        return this.field.isValid();
    }

    public boolean isBot() {
        return this.isBot;
    }

    public GameField getField() {
        return this.field;
    }

    public GameFieldProperties getFieldProperties() {
        return this.field.getProperties();
    }

    public void setOnline() {
        this.offlineTime = null;
    }

    public void setOffline() {
        this.offlineTime = new Date().getTime();
    }

    public Long getOfflineDuration() {
        if (this.offlineTime == null) {
            return 0L;
        }

        return new Date().getTime() - this.offlineTime;
    }
}