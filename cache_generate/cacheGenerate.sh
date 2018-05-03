#!/bin/bash
set -e

echo "Removing the old application-cache-generate.properties"
rm -rf "application-cache-generate.properties"
rm -rf "cacheGenerateSchema.sql"

cacheGenerateFileProperties="application-cache-generate.properties"
currentApplicationFileName="";
databaseType="";

updateMySqlPropertiesFile() 
{
    updated=""
    for line in `cat $cacheGenerateFileProperties`
    do
        updated+="$line\n"
    done
}

updateOraclePropertiesFile() 
{   
    updated=""
    for line in `cat $cacheGenerateFileProperties`
    do  
        if [[ "$line" =~ "spring.datasource.url" ]]
        then
            updated+=$(echo "$line" | cut -d'@' -f 1)
            updated+="@$dataBaseUrl:$dataBasePort:$dataBaseName\n"
        elif [[ "$line" =~ "spring.datasource.password" ]] ||  [[ "$line" =~ "spring.datasource.username" ]]
        then
            updated+=$(echo "$line" | cut -d'=' -f 1);
            updated+="=$cacheGenerateSchema\n"
        else
            updated+="$line\n"
        fi
    done
    echo -e "$updated" > $cacheGenerateFileProperties;
}

mySqlCacheGenerateSchema() 
{
    echo "Removing the old dump..."
    rm -rf $dumpName
    echo "Creating a new dump from: $dataBaseUrl:$dataBasePort, schema: $dataBaseName"
    mysqldump -h $dataBaseUrl --port $dataBasePort -u $dataBaseUserName -p$dataBaseUserPassword $dataBaseName > $dumpName

    if [ ! -f "$dumpName" ]
    then 
        echo "Dump file $dumpName not found" >&2
        return -1
    fi

    echo "Dropping the old schema ..."
    mysqladmin -u$dataBaseUserName -p$dataBaseUserPassword drop $cacheGenerateSchema
    echo "Creating a new schema..."
    echo "create database $cacheGenerateSchema" > $cacheGenerateSchemaFile
    mysql -u$dataBaseUserName -p$dataBaseUserPassword < $cacheGenerateSchemaFile
    echo "Importing the dump..."
    mysql -u$dataBaseUserName -p$dataBaseUserPassword $cacheGenerateSchema < $dumpName
    updateMySqlPropertiesFile
}

mySqlProperties() {
    dumpName="taskboard_cache_generate.dump"
    driverClassName=$(cat $cacheGenerateFileProperties | grep "spring.datasource.driverClassName")
    dataBaseUrlFull=$(cat $cacheGenerateFileProperties | grep "spring.datasource.url" | cut -d'/' -f 3)
    dataBaseUrl=$( echo $dataBaseUrlFull | cut -d':' -f 1)
    dataBasePort=$( echo $dataBaseUrlFull | cut -d':' -f 2)
    dataBaseName=$(cat $cacheGenerateFileProperties | grep "spring.datasource.url" | cut -d'/' -f 4 | cut -d'?' -f 1)
    dataBaseUserPassword=$(cat $cacheGenerateFileProperties | grep "spring.datasource.password" | cut -d'=' -f 2)
    dataBaseUserName=$(cat $cacheGenerateFileProperties | grep "spring.datasource.username" | cut -d'=' -f 2)
}

oracleProperties() {
    dataBaseUrlFull=$(cat $cacheGenerateFileProperties | grep "spring.datasource.url")
    dataBaseUrl=$( echo $dataBaseUrlFull | cut -d'@' -f 2 | cut -d ':' -f 1)
    dataBaseName=$( echo $dataBaseUrlFull | cut -d'@' -f 2 | cut -d ':' -f 3)
    dataBasePort=$( echo $dataBaseUrlFull | cut -d'@' -f 2 | cut -d ':' -f 2)
    dataBaseUserPassword=$(cat $cacheGenerateFileProperties | grep "spring.datasource.password" | cut -d'=' -f 2)
    dataBaseUserName=$(cat $cacheGenerateFileProperties | grep "spring.datasource.username" | cut -d'=' -f 2)
}

oracleCacheGenerateSchema() {
    dataBaseLinkName="cache_generate_link";
    masterSql="dropAndCreate.sql"
    oracleCredentials="$dataBaseUserName/$dataBaseUserPassword"
    oracleBase=$dataBaseUrl:$dataBasePort/$dataBaseName
    
    if [ -z "$1" ]
    then
        echo "We need a user that have permission to do some GRANTS, please input one:"
        read master
        echo "Password"
        read -s masterPassword;

    else
        master=$1;
        if [ -z "$2" ]
        then
            echo "Password for user $master:"
            read -s masterPassword;
        else
            masterPassword=$2
        fi
    fi

    echo "Droping schema and reacreating"
    echo "
        DROP USER $cacheGenerateSchema CASCADE;
        CREATE USER $cacheGenerateSchema IDENTIFIED BY $cacheGenerateSchema;
        GRANT CONNECT, DBA TO $cacheGenerateSchema;
        GRANT CREATE SESSION TO $cacheGenerateSchema;
        GRANT EXECUTE ANY PROCEDURE TO $cacheGenerateSchema;
    " > $masterSql
    echo @$masterSql | sqlplus "$master/$masterPassword@$oracleBase"

    echo "Creating a database link..."
    echo "
        DROP PUBLIC DATABASE LINK $dataBaseLinkName;
        CREATE PUBLIC DATABASE LINK $dataBaseLinkName CONNECT TO $cacheGenerateSchema IDENTIFIED BY $cacheGenerateSchema USING 'localhost:$dataBasePort/$dataBaseName';
    " > $cacheGenerateSchemaFile

    echo @$cacheGenerateSchemaFile | sqlplus "$oracleCredentials@$oracleBase"

    echo "Coping the schema: $dataBaseUserName, to: $cacheGenerateSchema ..."
    echo "alu?"
    impdp "$oracleCredentials@$oracleBase" schemas=$dataBaseUserName network_link=$dataBaseLinkName remap_schema=$dataBaseUserName:$cacheGenerateSchema
    echo "ok"
    echo "$cacheGenerateSchema has been created"
    updateOraclePropertiesFile
}

if [ -z "$1"  ]
then
    currentApplicationFileName="application-dev.properties";
    echo "Properties file name not provided, assuming $currentApplicationFileName"
else
    currentApplicationFileName="$1";
fi

echo "Using properties file $currentApplicationFileName"

cp $currentApplicationFileName $cacheGenerateFileProperties

if [ ! -f "$cacheGenerateFileProperties" ]
then
    echo "Properties file is not present"
    exit -1
fi

cacheGenerateSchema="taskboard_cache_generate"
cacheGenerateSchemaFile="cacheGenerateSchema.sql"
dataBaseUrlFull=$(cat $cacheGenerateFileProperties | grep "spring.datasource.url")

if [[ "$dataBaseUrlFull" =~ "mysql" ]]
then
    mySqlProperties
    mySqlCacheGenerateSchema
elif [[ "$dataBaseUrlFull" =~ "oracle" ]]
then
    oracleProperties
    oracleCacheGenerateSchema $2 $3
else
    echo "$dataBaseUrlFull not supported"
    exit -1
fi
