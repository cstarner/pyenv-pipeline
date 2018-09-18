# Dockerfile used to generate a way to test the plugins behavior on Linux
FROM jenkins/jenkins
USER root
RUN apt-get update && apt-get install -y python2.7 python-pip python3.4 python3-pip
RUN pip install virtualenv
RUN pip3 install virtualenv