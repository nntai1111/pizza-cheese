package pizza_cheese.todo.dao;

import java.io.IOException;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import pizza_cheese.todo.util.SqlLoader;
import pizza_cheese.todo.util.SqlQueries;

/**
 * Base class for JDBC DAOs that load SQL from a classpath resource file.
 */
public abstract class SqlDaoSupport {

    protected final NamedParameterJdbcTemplate jdbc;
    protected final SqlQueries queries;

    protected SqlDaoSupport(
            NamedParameterJdbcTemplate jdbc,
            ResourceLoader resourceLoader,
            String sqlClasspathLocation) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader, sqlClasspathLocation);
    }
}
