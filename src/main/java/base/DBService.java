package base;

import base.datasets.UserDataSet;
import com.sun.istack.internal.NotNull;
import dbservice.DatabaseException;

import java.util.List;

public interface DBService {
    public <T>T doReturningWork(@NotNull HibernateUnit<T> work)
            throws DatabaseException;

    public void doWork(@NotNull HibernateUnitVoid work)
            throws DatabaseException;

    public void shutdown();
}
