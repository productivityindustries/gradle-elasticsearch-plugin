package cgoit.gradle.elasticsearch

import org.gradle.api.Plugin
import org.gradle.api.Project

class ElasticsearchPlugin implements Plugin<Project> {
    static final String DEFAULT_ELASTICSEARCH_VERSION = '7.3.0'
    static final String DEFAULT_ELASTICSEARCH_SCHEME = 'http'
    static final String DEFAULT_ELASTICSEARCH_HOST = 'localhost'
    static final Integer DEFAULT_ELASTICSEARCH_PORT = 9200
    static final Integer DEFAULT_ELASTICSEARCH_TRANSPORT_PORT = 9300

    static final String ESC = "${(char) 27}"
    static final String CYAN = "${ESC}[36m"
    static final String GREEN = "${ESC}[32m"
    static final String YELLOW = "${ESC}[33m"
    static final String RED = "${ESC}[31m"
    static final String NORMAL = "${ESC}[0m"

    @Override
    void apply(Project project) {
        project.extensions.create('startElasticsearch', StartElasticsearchExtension, project)
        project.extensions.create('stopElasticsearch', StopElasticsearchExtension, project)
    }
}
