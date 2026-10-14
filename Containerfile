FROM registry.access.redhat.com/jboss-webserver-5/jws58-openjdk8-openshift-rhel8:latest
COPY target/*.war /deployments/ROOT.war
