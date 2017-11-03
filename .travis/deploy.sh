#!/bin/bash
cd `dirname $0`/.. 

if [ -z "$SONATYPE_USERNAME" ]
then
    echo "ERROR! Please set SONATYPE_USERNAME and SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]
then
    echo "ERROR! Please set SONATYPE_PASSWORD environment variable"
    exit 1
fi


if [ ! -z "$TRAVIS_TAG" ]
then
    export AGENT_JAR_VERSION=$TRAVIS_TAG
    SKIP_GPG_SIGN=false
    echo "travis tag is set -> updating pom.xml <version> attribute to $TRAVIS_TAG"
    mvn --settings .travis/settings.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=$TRAVIS_TAG 1>/dev/null 2>/dev/null
else
    SKIP_GPG_SIGN=true
    # extract the agent jar version from pom.xml
    export AGENT_JAR_VERSION=`cat pom.xml | sed -n -e 's/.*<version>\(.*\)<\/version>.*/\1/p' | head -1`
    echo "no travis tag is set, hence keeping the snapshot version in pom.xml"
fi

mvn clean deploy --settings .travis/settings.xml -Dgpg.skip=$SKIP_GPG_SIGN -DskipTests=true -B -U

echo "successfully deployed the jars to nexus"

./.travis/publish-to-docker-hub.sh


