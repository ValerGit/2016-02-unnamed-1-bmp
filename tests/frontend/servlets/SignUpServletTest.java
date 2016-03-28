package frontend.servlets;

import base.UserService;
import base.datasets.UserDataSet;
import dbservice.DatabaseException;
import dbservice.UserServiceImpl;
import org.hamcrest.core.StringContains;
import base.DBService;
import base.AccountService;
import main.AccountServiceImpl;
import dbservice.DBServiceImpl;
import main.Context;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;


public class SignUpServletTest {
    private Context context;
    private UserService userService;

    private HttpServletResponse getMockedResponse(StringWriter stringWriter) throws IOException {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final PrintWriter writer = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(writer);

        return response;
    }


    private HttpServletRequest getMockedRequest() {
        final HttpSession httpSession = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getSession()).thenReturn(httpSession);

        return request;
    }


    @Before
    public void setUp() throws Exception {
        context = new Context();
        userService = mock(UserServiceImpl.class);
        context.add(UserService.class, userService);
    }


    @Test
    public void testDoPost() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        final AccountService accountService = mock(AccountServiceImpl.class);
        context.add(AccountService.class, accountService);

        final String input = "{\"login\":\"admin\",\"password\":\"admin\", \"email\":\"admin@admin.com\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getReader()).thenReturn(bufferedReader);
        when(userService.getUserByLogin("admin")).thenReturn(newUser);
        when(userService.saveUser(any(UserDataSet.class))).thenReturn(1L);
        when(newUser.getId()).thenReturn(1L);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"id\":1"));
    }


    @Test
    public void testDoPostCantParse() throws IOException, ServletException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        final AccountService accountService = mock(AccountServiceImpl.class);
        context.add(AccountService.class, accountService);

        final String input = "{login\":\"admin\",\"password:\"admin\", \"email:\"admin@admin.com\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));

        when(request.getReader()).thenReturn(bufferedReader);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"error\":\"Wrong JSON\""));
    }


    @Test
    public void testDoPostEmptyField() throws IOException, ServletException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();
        final AccountService accountService = mock(AccountServiceImpl.class);
        context.add(AccountService.class, accountService);

        final String input = "{\"login\":\"admin\",\"password\":\"admin\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));

        when(request.getReader()).thenReturn(bufferedReader);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"error\":\"Not all params send\""));
    }


    @Test
    public void testDoPostAlreadyExist() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        final String input = "{\"login\":\"admin\",\"password\":\"admin\", \"email\":\"admin@admin.com\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));

        when(request.getReader()).thenReturn(bufferedReader);
        when(userService.saveUser(any(UserDataSet.class))).thenReturn(-1L);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"error\":\"Login already exist\""));
    }


    @Test
    public void testDoGet() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getPathInfo()).thenReturn("/1");
        when(userService.getUserById(anyLong())).thenReturn(newUser);
        when(newUser.getId()).thenReturn(1L);
        when(newUser.getLogin()).thenReturn("admin");
        when(newUser.getEmail()).thenReturn("admin");

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"id\":1"));
    }


    @Test
    public void testDoGetNoId() throws IOException, ServletException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        when(request.getPathInfo()).thenReturn("");

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }


    @Test
    public void testDoPut() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        final String input = "{\"login\":\"admin\",\"password\":\"admin\", \"email\":\"admin@admin.com\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getReader()).thenReturn(bufferedReader);
        when(request.getPathInfo()).thenReturn("/1");
        when(newUser.getId()).thenReturn(1L);
        when(userService.updateUserInfo(anyLong(), anyString(), anyString(), anyString())).thenReturn(true);
        when(userService.getUserById(1L)).thenReturn(newUser);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"id\":1"));
    }


    @Test
    public void testDoPutWrongId() throws IOException, ServletException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        when(request.getPathInfo()).thenReturn("/admin");

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }


    @Test
    public void testDoPutNotAllParams() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();

        final String input = "{\"login\":\"admin\", \"email\":\"admin@admin.com\"}";
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)));
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getReader()).thenReturn(bufferedReader);
        when(request.getPathInfo()).thenReturn("/1");
        when(newUser.getId()).thenReturn(1L);
        when(userService.updateUserInfo(anyLong(), anyString(), anyString(), anyString())).thenReturn(true);
        when(userService.getUserById(1L)).thenReturn(newUser);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"error\":\"Not all params send\""));
    }


    @Test
    public void testDoDelete() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getPathInfo()).thenReturn("/1");
        when(userService.getUserById(anyLong())).thenReturn(newUser);
        when(userService.deleteUserById(anyLong())).thenReturn(true);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertThat(stringWriter.toString(), StringContains.containsString("{}"));
    }


    @Test
    public void testDoDeleteFail() throws IOException, ServletException, DatabaseException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getPathInfo()).thenReturn("/1");
        when(userService.getUserById(anyLong())).thenReturn(newUser);
        when(userService.deleteUserById(anyLong())).thenReturn(false);

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }


    @Test
    public void testDoDeleteNumExept() throws IOException, ServletException {
        final StringWriter stringWriter = new StringWriter();
        final HttpServletResponse response = getMockedResponse(stringWriter);
        final HttpServletRequest request = getMockedRequest();
        final UserDataSet newUser = mock(UserDataSet.class);

        when(request.getPathInfo()).thenReturn("/111111111111111111111111");

        final SignUpServlet signUpServlet = new SignUpServlet(context);
        signUpServlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(stringWriter.toString(), StringContains.containsString("{\"error\":\"Wrong request\""));
    }


    @Test
    public void testIsInteger() throws Exception {
        final boolean ret = SignUpServlet.isInteger("27", 10);
        assertEquals(ret, true);
    }


    @Test
    public void testIsIntegerFail() throws Exception {
        final boolean ret = SignUpServlet.isInteger("lalala", 10);
        assertEquals(ret, false);
    }

}