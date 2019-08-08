package cgoit.gradle.elasticsearch

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.DEFAULT_ELASTICSEARCH_HOST
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.CYAN
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.DEFAULT_ELASTICSEARCH_VERSION
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.NORMAL
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.RED
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.YELLOW
import static org.apache.http.client.fluent.Executor.newInstance
import static org.apache.http.client.fluent.Request.Post

class StopElasticsearchAction {

    @Input
    @Optional
    private String httpHost

    @Input
    @Optional
    private Integer httpPort

    @Input
    @Optional
    File toolsDir

    @Input
    @Optional
    String elasticVersion

    private AntBuilder ant
    private Project project

    StopElasticsearchAction(Project project) {
        this.project = project
        this.ant = project.ant
    }

    void execute() {
        File toolsDir = toolsDir ?: new File("$project.rootDir/gradle/tools")
        ElasticsearchActions elastic = new ElasticsearchActions(project, toolsDir,
                elasticVersion ?: DEFAULT_ELASTICSEARCH_VERSION)

        println "${CYAN}* elastic:$NORMAL stopping ElasticSearch"

        try {
            if (Integer.valueOf(elastic.version.split("\\.")[0]) >= 2) {
                def pidFile = new File(elastic.home, 'elastic.pid')
                if (!pidFile.exists()) {
                    println "${RED}* elastic:$NORMAL ${pidFile} not found"
                    println "${RED}* elastic:$NORMAL could not stop ElasticSearch, please check manually!"
                    return
                }
                def elasticPid = pidFile.text
                println "${CYAN}* elastic:$NORMAL going to kill pid $elasticPid"

                "kill $elasticPid".execute()
            } else {
                newInstance().
                        execute(Post("http://${httpHost ?: DEFAULT_ELASTICSEARCH_HOST}:${httpPort ?: 9200}/_shutdown"))
            }

            println "${CYAN}* elastic:$NORMAL waiting for ElasticSearch to shutdown"
            ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "elasticTimeout") {
                not {
                    ant.http(url: "http://${httpHost ?: DEFAULT_ELASTICSEARCH_HOST}:${httpPort ?: 9200}")
                }
            }

            if (ant.properties['elasticTimeout'] != null) {
                println "${RED}* elastic:$NORMAL could not stop ElasticSearch"
                throw new RuntimeException("failed to stop ElasticSearch")
            } else {
                println "${CYAN}* elastic:$NORMAL ElasticSearch is now down"
            }
        } catch (ConnectException e) {
            println "${CYAN}* elastic:$YELLOW warning - unable to stop elastic on http port ${httpPort ?: 9200}, ${e.message}$NORMAL"
        }
    }

    StopElasticsearchAction withToolsDir(File toolsDir) {
        this.toolsDir = toolsDir
        return this
    }

    StopElasticsearchAction withElasticVersion(String elasticVersion) {
        this.elasticVersion = elasticVersion
        return this
    }

    StopElasticsearchAction withHttpHost(String httpHost) {
        this.httpHost = httpHost
        return this
    }

    StopElasticsearchAction withHttpPort(Integer httpPort) {
        this.httpPort = httpPort
        return this
    }
}
