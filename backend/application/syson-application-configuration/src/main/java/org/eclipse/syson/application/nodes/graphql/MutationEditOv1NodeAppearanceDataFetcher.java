package org.eclipse.syson.application.nodes.graphql;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.sirius.components.annotations.spring.graphql.MutationDataFetcher;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.graphql.api.IDataFetcherWithFieldCoordinates;
import org.eclipse.sirius.components.graphql.api.IEditingContextDispatcher;
import org.eclipse.sirius.components.graphql.api.IExceptionWrapper;
import org.eclipse.syson.application.nodes.dto.EditOv1NodeAppearanceInput;

import graphql.schema.DataFetchingEnvironment;
import tools.jackson.databind.ObjectMapper;

@MutationDataFetcher(type = "Mutation", field = "editOv1NodeAppearance")
public class MutationEditOv1NodeAppearanceDataFetcher implements IDataFetcherWithFieldCoordinates<CompletableFuture<IPayload>> {

    private static final String INPUT_ARGUMENT = "input";
    private final ObjectMapper objectMapper;
    private final IExceptionWrapper exceptionWrapper;
    private final IEditingContextDispatcher editingContextDispatcher;

    public MutationEditOv1NodeAppearanceDataFetcher(ObjectMapper objectMapper, IExceptionWrapper exceptionWrapper, IEditingContextDispatcher editingContextDispatcher) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.exceptionWrapper = Objects.requireNonNull(exceptionWrapper);
        this.editingContextDispatcher = Objects.requireNonNull(editingContextDispatcher);
    }

    @Override
    public CompletableFuture<IPayload> get(DataFetchingEnvironment environment) throws Exception {
        Object argument = environment.getArgument(INPUT_ARGUMENT);
        var input = this.objectMapper.convertValue(argument, EditOv1NodeAppearanceInput.class);
        return this.exceptionWrapper.wrapMono(() -> this.editingContextDispatcher.dispatchMutation(input.editingContextId(), input), input).toFuture();
    }
}
