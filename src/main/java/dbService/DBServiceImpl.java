package dbservice;

import base.DBService;
import base.HibernateUnit;
import base.HibernateUnitVoid;
import base.datasets.UserDataSet;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import main.LaunchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.ServiceRegistry;


public class DBServiceImpl implements DBService {
    private SessionFactory sessionFactory;
    private static final Logger LOGGER = LogManager.getLogger(DBServiceImpl.class);

    public DBServiceImpl(Configuration configuration) throws HibernateException, LaunchException {
        configuration.addAnnotatedClass(UserDataSet.class);

        LOGGER.info("Configuring database...");
        final StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        builder.applySettings(configuration.getProperties());
        try {
            final ServiceRegistry serviceRegistry = builder.build();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (HibernateException e) {
            throw new LaunchException("Failed to create hibernate session factory", e);
        }
    }

    @Override
    @Nullable
    public <T>T doReturningWork(@NotNull HibernateUnit<T> work) throws DatabaseException {
        final Session session = sessionFactory.openSession();
        try {
            session.getTransaction().begin();
            final T result = work.operate(session);
            session.getTransaction().commit();
            return result;
        } catch ( RuntimeException e ) {
            if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE
                    || session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
                session.getTransaction().rollback();
            }
            throw new DatabaseException("Fail to perform database transaction",e);
        } finally {
            session.close();
        }
    }

    @Override
    @Nullable
    public void doWork(@NotNull HibernateUnitVoid work) throws DatabaseException {
        final Session session = sessionFactory.openSession();
        try {
            session.getTransaction().begin();
            work.operate(session);
            session.getTransaction().commit();
        } catch ( RuntimeException e ) {
            if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE
                    || session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
                session.getTransaction().rollback();
            }
            throw new DatabaseException("Fail to perform database transaction",e);
        } finally {
            session.close();
        }
    }

    @Override
    public void shutdown() {
        sessionFactory.close();
        LOGGER.info("Shutdown database connection");
    }
}