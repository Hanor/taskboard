## The Mechanism
The cacheGenerate.sh was devoloped to make easy the generation of issues cache without stop the application.
This mechanism actualy only support *oracle* and *mysql*.

## The Steps to use
To use this mechanism you will need follow some steps:
- First of all this mechanisms needs the client of the database provider to make the dumps/create/generate the information to make the cache then:
    - if your database provider is mysql, you need to install the mysql client, in this link you can do it:
    https://dev.mysql.com/doc/mysql-getting-started/en/;
    - if your database provider is oracle, you need to install the oracle client, you can do it following this steps:
    (if the SO is not a RPM like CentOs/RedHat the installation of oracle is not simple )
    * The downloads of the oracle client need to be the same database version!!!

to a application properties, you can pass the path of the application properties as a parameter:
Ex: ./cacheGenerate.sh /home/hanor.cintra/application-dev.properties