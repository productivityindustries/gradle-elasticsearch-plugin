package cgoit.gradle.elasticsearch

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StopElasticsearchExtension implements Configurable<StopElasticsearchExtension> {
    private Project project

    StopElasticsearchExtension(Project project) {
        this.project = project
    }

    @Override
    StopElasticsearchExtension configure(Closure closure) {
        configure(closure, new StopElasticsearchAction(project)).execute()

        return this
    }
}
