package base;


import base.datasets.UserDataSet;
import dbservice.DatabaseException;

import java.util.List;

public interface UserService {
    void saveUser (UserDataSet dataSet) throws DatabaseException;

    UserDataSet getUserById (long id) throws DatabaseException;

    UserDataSet getUserByEmail(String email) throws DatabaseException;

    UserDataSet getUserByLogin(String login) throws DatabaseException;

    boolean updateUserInfo(Long id, String login, String pass) throws DatabaseException;

    List<UserDataSet> getUsers() throws DatabaseException;

    boolean isEmailUnique(String email) throws DatabaseException;

    boolean isLoginUnique(String login) throws DatabaseException;
}
