package lu.r3flexi0n.bungeeonlinetime.repository;

import javax.sql.DataSource;

public abstract class Repository {
    protected final DataSource dataSource;

    public Repository(DataSource dataSource) {
        this.dataSource = dataSource;
        init();
    }

    protected abstract void init();
}
