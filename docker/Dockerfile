# FROM quay.io/centos/centos:stream8
FROM bellsoft/liberica-openjdk-centos:13.0.2-9
#RUN dnf upgrade --setopt=install_weak_depts=False -y  && dnf clean all
RUN yum -y install initscripts && yum -y install git && yum -y install wget && yum -y install telnet && yum -y install net-tools
RUN yum clean all && yum clean metadata && rm -rf /var/cache/yum/*
VOLUME /tmp
ARG JAR_FILE
ARG PROFILES
ENV SET_PROFILE="$PROFILES"
RUN mkdir /app
WORKDIR /app
COPY ${JAR_FILE} inner-gateway-api.jar
COPY deploy/entrypoint.sh entrypoint.sh
RUN chmod 755 entrypoint.sh
RUN mkdir /app/logs
RUN mkdir /app/logs/error
RUN touch /app/logs/spring.log
RUN touch /app/logs/error/spring-error.log

ARG AWS_ACCESS_KEY_ID
ARG AWS_SECRET_ACCESS_KEY
ARG AWS_SESSION_TOKEN
RUN echo $AWS_ACCESS_KEY_ID
RUN echo $AWS_SECRET_ACCESS_KEY
RUN echo $AWS_SESSION_TOKEN

EXPOSE 7070/tcp
#ENTRYPOINT ["sh", "entrypoint.sh", ${PROFILES}]
ENTRYPOINT sh entrypoint.sh $SET_PROFILE $AWS_ACCESS_KEY_ID $AWS_SECRET_ACCESS_KEY $AWS_SESSION_TOKEN