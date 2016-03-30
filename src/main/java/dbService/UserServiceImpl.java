package dbservice;

import base.DBService;
import base.UserService;
import base.datasets.UserDataSet;
import dbservice.dao.UserDataSetDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;

import java.util.List;


public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);
    private DBService dbService;

    public UserServiceImpl(DBService dbService) throws HibernateException {
        this.dbService = dbService;
    }

    @Override
    public void saveUser(UserDataSet dataSet) throws DatabaseException {
        dbService.doWork((session)-> {
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            dao.save(dataSet);
        });
    }

    @Override
    public UserDataSet getUserById(long id) throws DatabaseException {
        return dbService.doReturningWork((session) -> {
            LOGGER.info("Get user info with id {}", id);
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.readById(id);
        });
    }

    @Override
    public UserDataSet getUserByEmail(String email) throws DatabaseException {
        return dbService.doReturningWork((session) -> {
            LOGGER.info("Get user info with email {}", email);
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.readByEmail(email);
        });
    }


    @Override
    public UserDataSet getUserByLogin(String login) throws DatabaseException {
        return dbService.doReturningWork((session) -> {
            LOGGER.info("Get user info with login {}", login);
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.readByLogin(login);
        });
    }


    @Override
    public boolean updateUserInfo(Long id, String login, String pass) throws DatabaseException {
        return dbService.doReturningWork((session)-> {
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            final boolean succeed = dao.updateUserInfo(id, login, pass);
            if (!succeed) {
                LOGGER.error("Fail to update user #{}", id);
                return false;
            }
            LOGGER.info( "Updated user #{}  with info: {}, {}", id, login);
            return true;
        });
    }

    @Override
    public List<UserDataSet> getUsers() throws DatabaseException {
        return dbService.doReturningWork((session)-> {
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.readAll();
        });
    }

    @Override
    public boolean isEmailUnique(String email) throws DatabaseException {
        return dbService.doReturningWork((session)-> {
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.checkUniqueEmail(email);
        });
    }

    @Override
    public boolean isLoginUnique(String login) throws DatabaseException {
        return dbService.doReturningWork((session)-> {
            final UserDataSetDAO dao = new UserDataSetDAO(session);
            return dao.checkUniqueLogin(login);
        });
    }
}
