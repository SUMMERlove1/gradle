/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.problems


import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class ProblemsServiceIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        enableProblemsApiCheck()
        buildFile """
            tasks.register("reportProblem", ProblemReportingTask)
        """
    }

    def "can emit a problem with mandatory fields"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .stackLocation()
                        .category("type")
                    }
                }
            }
        """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["label"] == "label"
        problem.details["problemCategory"]["category"] == "type"
        problem.details["locations"][0] == [length:-1, column:-1, line:14, path: "build file '$buildFile.absolutePath'"]
        problem.details["locations"][1] == [
            buildTreePath: ":reportProblem"
        ]
    }

    def "can emit a problem with user-manual documentation"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .documentedAt("https://example.org/doc")
                        .category("type")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["documentationLink"]["url"] == 'https://example.org/doc'
    }

    def "can emit a problem with upgrade-guide documentation"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation


            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .documentedAt("https://docs.example.org/test-section")
                        .category("type")
                        }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["documentationLink"]["url"] == 'https://docs.example.org/test-section'
    }

    def "can emit a problem with dsl-reference documentation"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.internal.InternalProblems
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .documentedAt("https://example.org/doc")
                        .category("type")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["documentationLink"]["url"] == 'https://example.org/doc'
    }

    def "can emit a problem with partially specified location"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .offsetInFileLocation("test-location", 1, 2)
                        .category("type")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["locations"][0] == [
            "path": "test-location",
            "offset": 1,
            "length": 2
        ]
    }

    def "can emit a problem with fully specified location"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .offsetInFileLocation("test-location", 1, 2)
                        .category("type")
                    }
                }
            }
            """

        when:
        run("reportProblem")


        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["locations"][0] == [
            "path": "test-location",
            "offset": 1,
            "length": 2
        ]
        problem.details["locations"][1] == [
            "buildTreePath": ":reportProblem"
        ]
    }

    def "can emit a problem with plugin location specified"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .pluginLocation("org.example.pluginid")
                        .category("type")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]

        def fileLocation = problem.details["locations"][0]
        fileLocation["pluginId"] == "org.example.pluginid"
    }

    def "can emit a problem with a severity"(Severity severity) {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .category("type")
                        .solution("solution")
                        .severity(Severity.${severity.name()})
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["severity"] == severity.name()

        where:
        severity << Severity.values()
    }

    def "can emit a problem with a solution"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.ProblemReporter
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .category("type")
                        .solution("solution")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["solutions"] == [
            "solution"
        ]
    }

    def "can emit a problem with exception cause"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.ProblemReporter
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .category("type")
                        .withException(new RuntimeException("test"))
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["exception"]["message"] == "test"
        !(problem.details["exception"]["stackTrace"] as List<String>).isEmpty()
    }

    def "can emit a problem with additional data"() {
        given:
        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.ProblemReporter
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .category("type")
                        .additionalData("key", "value")
                    }
                }
            }
            """

        when:
        run("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["additionalData"] == [
            "key": "value"
        ]
    }

    def "cannot emit a problem with invalid additional data"() {
        given:
        disableProblemsApiCheck()

        buildFile """
            import org.gradle.api.problems.internal.Problem
            import org.gradle.api.problems.ProblemReporter
            import org.gradle.api.problems.Severity
            import org.gradle.internal.deprecation.Documentation

            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").reporting {
                        it.label("label")
                        .category("type")
                        .additionalData("key", ["collections", "are", "not", "supported", "yet"])
                    }
                }
            }
            """

        when:
        def failure = fails("reportProblem")

        then:
        failure.assertHasCause('ProblemBuilder.additionalData() supports values of type String, but java.util.ArrayList as given.')
    }

    def "can throw a problem with a wrapper exception"() {
        given:
        buildFile """
            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    problems.forNamespace("org.example.plugin").throwing {
                        spec -> spec
                            .label("label")
                            .category("type")
                            .withException(new RuntimeException("test"))
                    }
                }
            }
            """

        when:

        fails("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["exception"]["message"] == "test"
    }

    def "can rethrow a problem with a wrapper exception"() {
        given:
        buildFile """
            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    def exception = new RuntimeException("test")
                    problems.forNamespace("org.example.plugin").rethrowing(exception) { it
                        .label("label")
                        .category("type")
                    }
                }
            }
            """

        when:
        fails("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 1
        def problem = problems[0]
        problem.details["exception"]["message"] == "test"
    }

    def "can rethrow a problem with a wrapper exception"() {
        given:
        buildFile """
            abstract class ProblemReportingTask extends DefaultTask {
                @Inject
                protected abstract Problems getProblems();

                @TaskAction
                void run() {
                    try {
                        def exception = new RuntimeException("test")
                        problems.forNamespace("org.example.plugin").throwing { spec -> spec
                            .label("inner")
                            .category("type")
                            .withException(exception)
                        }
                    } catch (RuntimeException ex) {
                        problems.forNamespace("org.example.plugin").rethrowing(ex) { spec -> spec
                            .label("outer")
                            .category("type")
                        }
                    }
                }
            }
            """

        when:
        fails("reportProblem")

        then:
        def problems = this.collectedProblems
        problems.size() == 2
        problems[0].details["label"] == "inner"
        problems[1].details["label"] == "outer"
    }
}
