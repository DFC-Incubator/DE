FROM discoenv/javabase

USER root
VOLUME ["/etc/iplant/de"]

COPY conf/main/logback.xml /
COPY target/saved-searches-standalone.jar /

ARG git_commit=unknown
ARG buildenv_git_commit=unknown
ARG version=unknown
LABEL org.iplantc.de.saved-searches.git-ref="$git_commit" \
      org.iplantc.de.saved-searches.version="$version" \
      org.iplantc.de.buildenv.git-ref="$buildenv_git_commit"

RUN ln -s "/opt/jdk/bin/java" "/bin/saved-searches"
ENTRYPOINT ["saved-searches", "-Dlogback.configurationFile=/etc/iplant/de/logging/saved-searches-logging.xml", "-cp", ".:saved-searches-standalone.jar", "saved_searches.core"]
CMD ["--help"]

