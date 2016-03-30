package dbservice;

import base.DBService;
import base.UserService;
import base.datasets.UserDataSet;
import dbservice.dao.UserDataSetDAO;
import main.LaunchException;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class UserServiceImplTest {
    UserService userService;
    DBService dbService;

    @Before
    public void toSet() throws LaunchException {
        dbService = new DBServiceImpl(new Configuration().configure("h2config.xml"));
        userService = new UserServiceImpl(dbService);
    }

    @After
    public void toReset() {
        dbService.shutdown();
    }

    @Test
    public void testSaveUser() throws DatabaseException {
        final UserDataSetDAO dao = mock(UserDataSetDAO.class);
        assertTrue(userService.saveUser(new UserDataSet(anyString(),anyString(), anyString())));
    }

    @Test
    public void testSaveUserDoubleLogin() throws DatabaseException {
        userService.saveUser(new UserDataSet("admin", "admin", "admin@admin.com"));
        assertFalse(userService.saveUser(new UserDataSet("admin", "adminNew", "adminNew@admin.com")));
    }


    @Test
    public void testUpdateUserInfo() throws DatabaseException {
        userService.saveUser(new UserDataSet("admin", "admin", "admin@admin.com"));
        final long addedId = userService.getUserByEmail("admin@admin.com").getId();
        assertTrue(userService.updateUserInfo(addedId, "adminNew", "adminNew@admin.com"));
        assertEquals("adminNew",userService.getUserById(addedId).getLogin());
    }

}