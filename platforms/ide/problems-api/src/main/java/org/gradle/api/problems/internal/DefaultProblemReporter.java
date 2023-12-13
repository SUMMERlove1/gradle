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

package org.gradle.api.problems.internal;

import org.gradle.api.Action;
import org.gradle.api.problems.ProblemSpec;
import org.gradle.internal.operations.CurrentBuildOperationRef;
import org.gradle.internal.operations.OperationIdentifier;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultProblemReporter implements InternalProblemReporter {

    private final ProblemEmitter emitter;
    private final List<ProblemTransformer> transformers;
    private final String namespace;

    public DefaultProblemReporter(ProblemEmitter emitter, List<ProblemTransformer> transformers, String namespace) {
        this.emitter = emitter;
        this.transformers = transformers;
        this.namespace = namespace;
    }

    @Override
    public void reporting(Action<ProblemSpec> spec) {
        DefaultProblemBuilder problemBuilder = createProblemBuilder();
        spec.execute(problemBuilder);
        // TODO (donat) instead of blowing up on misconfigured instances: https://github.com/gradle/gradle/issues/27353
        report(problemBuilder.build());
    }

    @Override
    public RuntimeException throwing(Action<ProblemSpec> spec) {
        DefaultProblemBuilder problemBuilder = createProblemBuilder();
        spec.execute(problemBuilder);
        Problem problem = problemBuilder.build();
        RuntimeException exception = problem.getException();
        // TODO (donat) instead of blowing up on misconfigured instances: https://github.com/gradle/gradle/issues/27353
        if (exception == null) {
            throw new IllegalStateException("Exception must be non-null");
        } else {
            throw throwError(exception, problem);
        }
    }

    public RuntimeException throwError(RuntimeException exception, Problem problem) {
        report(problem);
        throw exception;
    }

    @Override
    public RuntimeException rethrowing(RuntimeException e, Action<ProblemSpec> spec) {
        DefaultProblemBuilder problemBuilder = createProblemBuilder();
        spec.execute(problemBuilder);
        problemBuilder.withException(e);
        throw throwError(e, problemBuilder.build());
    }

    @Override
    public Problem create(Action<InternalProblemSpec> action) {
        DefaultProblemBuilder defaultProblemBuilder = createProblemBuilder();
        action.execute(defaultProblemBuilder);
        return defaultProblemBuilder.build();
    }

    // This method is only public to integrate with the existing task validation framework.
    // We should rework this integration and this method private.
    public DefaultProblemBuilder createProblemBuilder() {
        return new DefaultProblemBuilder(namespace);
    }

    private Problem transformProblem(Problem problem) {
        for (ProblemTransformer transformer : transformers) {
            problem = transformer.transform((InternalProblem) problem);
        }
        return problem;
    }

    /**
     * Reports a problem.
     * <p>
     * The current build operation is used as the operation identifier.
     * <p>
     * If there is no current build operation, the problem is not reported.
     *
     * @param problem The problem to report.
     */
    @Override
    public void report(Problem problem) {
        OperationIdentifier id = CurrentBuildOperationRef.instance().getId();
        report(problem, id);
    }

    /**
     * Reports a problem with an explicit operation identifier.
     * <p>
     * If the operation identifier is null, the problem is not reported.
     *
     * @param problem The problem to report.
     * @param id The operation identifier to associate with the problem.
     */
    @Override
    public void report(Problem problem, @Nullable OperationIdentifier id) {
        if (id != null) {
            emitter.emit(transformProblem(problem), id);
        }
    }
}
