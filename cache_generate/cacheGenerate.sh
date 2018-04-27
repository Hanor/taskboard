#!/bin/bash
set -e

echo "Removing the old application-cache-generate.properties"
rm -rf "application-cache-generate.properties"
rm -rf "cacheGenerateSchema.sql"

cacheGenerateFileName="application-cache-generate.properties"
currentApplicationFileName="";
databaseType="";

updateThePropertiesFile() 
{
    return
}

mySqlCacheGenerateSchema() 
{
    echo "Removing the old dump..."
    rm -rf $dumpName
    echo "Creating a new dump from: $dataBaseIp:$dataBasePort, schema: $dataBaseName"
    mysqldump -h $dataBaseIp --port $dataBasePort -u $dataBaseUserName -p$dataBaseUserPassword $dataBaseName > $dumpName

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
    updateThePropertiesFile
}

if [ -z "$1"  ]
then
    currentApplicationFileName="application-dev.properties";
    echo "Properties file name not provided, assuming $currentApplicationFileName"
else
    currentApplicationFileName="$1";
fi

echo "Using properties file $currentApplicationFileName"

cp $currentApplicationFileName $cacheGenerateFileName

if [ ! -f "$cacheGenerateFileName" ]
then
    echo "Properties file is not present"
    exit -1
fi

driverClassName=$(cat $cacheGenerateFileName | grep "spring.datasource.driverClassName")
dataBaseUrlFull=$(cat $cacheGenerateFileName | grep "spring.datasource.url" | cut -d'/' -f 3)
dataBaseIp=$( echo $dataBaseUrlFull | cut -d':' -f 1)
dataBasePort=$( echo $dataBaseUrlFull | cut -d':' -f 2)
dataBaseName=$(cat $cacheGenerateFileName | grep "spring.datasource.url" | cut -d'/' -f 4 | cut -d'?' -f 1)
dataBaseUserPassword=$(cat $cacheGenerateFileName | grep "spring.datasource.password" | cut -d'=' -f 2)
dataBaseUserName=$(cat $cacheGenerateFileName | grep "spring.datasource.username" | cut -d'=' -f 2)

dumpName="taskboard_cache_generate.dump"
cacheGenerateSchema="taskboard_cache_generate"
cacheGenerateSchemaFile="cacheGenerateSchema.sql"

if [[ "$driverClassName" =~ "mysql" ]]
then
    mySqlCacheGenerateSchema
else
    echo "$driverClassName not supported"
    exit -1
fi
