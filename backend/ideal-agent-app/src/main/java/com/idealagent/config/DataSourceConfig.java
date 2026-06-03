package com.idealagent.config;

import com.idealagent.properties.HikariDataSourceProperties;
import com.idealagent.properties.MySQLProperties;
import com.idealagent.properties.PostgreSQLProperties;
import com.idealagent.properties.SQLDataSourceProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({MySQLProperties.class, PostgreSQLProperties.class, HikariDataSourceProperties.class})
public class DataSourceConfig {
    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(MySQLProperties mysqlProperties, HikariDataSourceProperties hikariProperties) {
        return new HikariDataSource(hikariConfig(mysqlProperties, hikariProperties));
    }

    @Bean(name = "postgresqlDataSource")
    public DataSource postgresqlDataSource(PostgreSQLProperties postgreSQLProperties, HikariDataSourceProperties hikariProperties) {
        return new HikariDataSource(hikariConfig(postgreSQLProperties, hikariProperties));
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] mappers = resolver.getResources("classpath*:mapper/**/*.xml");
        if (mappers.length > 0) {
            factoryBean.setMapperLocations(mappers);
        }
        return factoryBean.getObject();
    }

    @Bean(name = "mysqlTemplate")
    public SqlSessionTemplate mysqlTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "postgresqlTemplate")
    public JdbcTemplate postgresqlTemplate(@Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private HikariConfig hikariConfig(SQLDataSourceProperties sqlProperties, HikariDataSourceProperties hikariProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(sqlProperties.getJdbcUrl());
        config.setUsername(sqlProperties.getUsername());
        config.setPassword(sqlProperties.getPassword());
        config.setDriverClassName(sqlProperties.getDriverClassName());
        config.setPoolName(sqlProperties.getPoolName());
        config.setMinimumIdle(hikariProperties.getMinimumIdle());
        config.setMaximumPoolSize(hikariProperties.getMaximumPoolSize());
        config.setIdleTimeout(hikariProperties.getIdleTimeout());
        config.setMaxLifetime(hikariProperties.getMaxLifetime());
        config.setConnectionTimeout(hikariProperties.getConnectionTimeout());
        config.setConnectionTestQuery(hikariProperties.getConnectionTestQuery());
        config.setInitializationFailTimeout(hikariProperties.getInitializationFailTimeout());
        return config;
    }
}
