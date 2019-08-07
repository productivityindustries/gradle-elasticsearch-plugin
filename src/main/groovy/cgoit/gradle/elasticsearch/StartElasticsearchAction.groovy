package cgoit.gradle.elasticsearch

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.CYAN
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.DEFAULT_ELASTICSEARCH_HOST
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.DEFAULT_ELASTICSEARCH_VERSION
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.NORMAL
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.RED
import static cgoit.gradle.elasticsearch.ElasticsearchPlugin.YELLOW
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class StartElasticsearchAction {

    @Input
    @Optional
    String elasticsearchVersion

    @Input
    @Optional
    String httpHost

    @Input
    @Optional
    Integer httpPort

    @Input
    @Optional
    Integer transportPort

    @Input
    @Optional
    File toolsDir

    @Input
    @Optional
    File dataDir

    @Input
    @Optional
    File logsDir

    private Project project

    private AntBuilder ant

    StartElasticsearchAction(Project project) {
        this.project = project
        this.ant = project.ant
    }

    void execute() {
        File toolsDir = toolsDir ?: new File("$project.rootDir/gradle/tools")
        ElasticsearchActions elastic = new ElasticsearchActions(project, toolsDir,
                elasticsearchVersion ?: DEFAULT_ELASTICSEARCH_VERSION)

        elastic.install()

        def pidFile = new File(elastic.home, 'elastic.pid')
        if (pidFile.exists()) {
            println "${YELLOW}* elastic:$NORMAL ElasticSearch seems to be running at pid ${pidFile.text}"
            println "${YELLOW}* elastic:$NORMAL please check $pidFile"
            return
        }

        httpPort = httpPort ?: 9200
        transportPort = transportPort ?: 9300
        dataDir = dataDir ?: new File("$project.buildDir/elastic")
        logsDir = logsDir ?: new File("$dataDir/logs")
        println "${CYAN}* elastic:$NORMAL starting ElasticSearch at $elastic.home using http port $httpPort and tcp transport port $transportPort"
        println "${CYAN}* elastic:$NORMAL ElasticSearch data directory: $dataDir"
        println "${CYAN}* elastic:$NORMAL ElasticSearch logs directory: $logsDir"

        ant.delete(failonerror: true, dir: dataDir)
        ant.delete(failonerror: true, dir: logsDir)
        logsDir.mkdirs()
        dataDir.mkdirs()

        def optPrefix = Integer.valueOf(elastic.version.split("\\.")[0]) >= 5 ? "-E" : "-Des."
        File esScript = new File("${elastic.home}/bin/elasticsearch${isFamily(FAMILY_WINDOWS) ? '.bat' : ''}")
        def command = [
                esScript.absolutePath,
                "${optPrefix}http.port=$httpPort",
                "${optPrefix}transport.tcp.port=$transportPort",
                "${optPrefix}path.data=$dataDir",
                "${optPrefix}path.logs=$logsDir"
        ]

        if (!isFamily(FAMILY_WINDOWS)) {
            command += [
                    "-p${pidFile}"
            ]
        }

        def esEnv = [
                "JAVA_HOME=${System.properties['java.home']}",
                "ES_HOME=$elastic.home"
        ]

        if (Integer.valueOf(elastic.version.split("\\.")[0]) >= 5) {
            esEnv += [
                    "ES_JAVA_OPTS=-Xms128m -Xmx512m"
            ]
        } else {
            esEnv += [
                    "ES_MAX_MEM=512m",
                    "ES_MIN_MEM=128m"
            ]
        }

        Process p = command.execute(esEnv, elastic.home)

        def out = new StringBuilder()
        p.consumeProcessOutput(out, out)

        println "${CYAN}* elastic:$NORMAL waiting for ElasticSearch to start"
        ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "elasticTimeout") {
            and {
                socket(server: "${httpHost ?: DEFAULT_ELASTICSEARCH_HOST}", port: transportPort)
                ant.http(url: "http://${httpHost ?: DEFAULT_ELASTICSEARCH_HOST}:$httpPort")
            }
        }

        println out

        if (ant.properties['elasticTimeout'] != null) {
            println "${RED}* elastic:$NORMAL could not start ElasticSearch"
            throw new RuntimeException("failed to start ElasticSearch")
        } else {
            println "${CYAN}* elastic:$NORMAL ElasticSearch is now up"
        }
    }
}
