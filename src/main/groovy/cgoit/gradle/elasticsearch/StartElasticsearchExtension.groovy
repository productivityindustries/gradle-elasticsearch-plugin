package cgoit.gradle.elasticsearch

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StartElasticsearchExtension implements Configurable<StartElasticsearchExtension> {
    private Project project

    StartElasticsearchExtension(Project project) {
        this.project = project
    }

    @Override
    StartElasticsearchExtension configure(Closure closure) {
        configure(closure, new StartElasticsearchAction(project)).execute()

        return this
    }
}
