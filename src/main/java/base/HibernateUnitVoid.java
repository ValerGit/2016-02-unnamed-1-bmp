package base;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.hibernate.Session;

@FunctionalInterface
public interface HibernateUnitVoid {
    @Nullable
    void operate(@NotNull Session session);
}
