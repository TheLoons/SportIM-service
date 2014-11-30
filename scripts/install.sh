#!/bin/bash
#
# Build and install the SportIM service. This script should be run from
# the scripts directory and requires the following variables to be set:
#
# Usage:
# install.sh <tomcat_home> <db_user> <db_password>
#   - tomcat_home: tomcat home directory (usually /var/lib/tomcat7 and
#                  includes the webapps directory
#   - db_user: the database username
#   - db_password: the database user's password
#
# Further configuration:
#   - to change the default database location, set the environment
#     variables: DB_HOST and DB_PORT

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
    echo "Usage: ./install.sh <tomcat_home> <db_user> <db_password>"
    exit 1;
fi

: ${DB_HOST:="cs4400-02.eng.utah.edu"}
: ${DB_PORT:="3306"}

pushd ../web/META-INF
sed -i "s/USER/$2/" context.xml
sed -i "s/PASSWORD/$3/" context.xml
sed -i "s/DB_HOST/$DB_HOST/" context.xml
sed -i "s/DB_PORT/$DB_PORT/" context.xml
popd

pushd ..
mvn clean package
popd

pushd ../target
cp sportim.war "$1/webapps"
popd

echo "Install finished. Restart your Tomcat instance to ensure complete installation."
