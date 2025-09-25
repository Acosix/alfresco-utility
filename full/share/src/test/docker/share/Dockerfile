FROM ${docker.tests.share.base}

COPY de.acosix.alfresco.utility.share*.amp ${docker.tests.share.ampPath}/
COPY slf4j-log4j12*.jar ${docker.tests.share.webappPath}/WEB-INF/lib/

RUN java -jar ${docker.tests.share.mmtPath}/alfresco-mmt*.jar install \
              ${docker.tests.share.ampPath}/de.acosix.alfresco.utility.share*.amp \
              ${docker.tests.share.webappPath} -nobackup

COPY share.xml ${docker.tests.share.tomcatPath}/conf/Catalina/localhost/share.xml
COPY share-config-custom.xml ${docker.tests.share.tomcatPath}/shared/classes/alfresco/web-extension/share-config-custom.xml
COPY dev-log4j.properties dev-log4j2.properties ${docker.tests.share.tomcatPath}/shared/classes/alfresco/web-extension/

RUN sed -i 's/<secure>true<\/secure>/<secure>false<\/secure>/' ${docker.tests.share.tomcatPath}/conf/web.xml
