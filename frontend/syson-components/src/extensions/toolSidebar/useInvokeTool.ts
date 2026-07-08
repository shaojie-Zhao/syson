/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/

import { gql, useMutation } from '@apollo/client';

/**
 * GraphQL mutation to invoke a palette tool on the given diagram elements.
 */
export const invokeToolMutation = gql`
  mutation invokeSingleClickOnDiagramElementTool($input: InvokeSingleClickOnDiagramElementToolInput!) {
    invokeSingleClickOnDiagramElementTool(input: $input) {
      __typename
      ... on ErrorPayload {
        message
      }
    }
  }
`;

/**
 * GraphQL mutation for delete-from-diagram tools (model-level deletion).
 */
export const deleteFromDiagramMutation = gql`
  mutation deleteFromDiagram($input: DeleteFromDiagramInput!) {
    deleteFromDiagram(input: $input) {
      __typename
      ... on ErrorPayload {
        message
      }
    }
  }
`;

/**
 * Tool IDs that perform semantic deletion (remove from model).
 * These use deleteFromDiagram mutation, not invokeSingleClickOnDiagramElementTool.
 * "Delete from Diagram" (visual removal) still uses invokeSingleClickOnDiagramElementTool.
 */
const SEMANTIC_DELETE_TOOL_IDS = ['semantic-delete'];

/**
 * Hook that returns a function to invoke a palette tool.
 *
 * Automatically selects the correct mutation based on tool ID:
 * - "semantic-delete" → deleteFromDiagram (model-level deletion)
 * - All other tools → invokeSingleClickOnDiagramElementTool
 *
 * @returns A tuple: [invokeTool function, { loading, error }]
 */
export const useInvokeTool = (): [
  (editingContextId: string, representationId: string, toolId: string, diagramElementIds: string[], toolLabel?: string, variables?: Array<{ name: string; value: string; type?: string }>) => void,
  { loading: boolean; error: string | null }
] => {
  const [invokeToolMutationFn, { loading: invokeLoading, error: invokeError }] = useMutation(
    invokeToolMutation
  );
  const [deleteMutationFn, { loading: deleteLoading, error: deleteError }] = useMutation(
    deleteFromDiagramMutation
  );

  const loading = invokeLoading || deleteLoading;
  const combinedError = invokeError?.message || deleteError?.message || null;

  const invokeTool = (
    editingContextId: string,
    representationId: string,
    toolId: string,
    diagramElementIds: string[],
    _toolLabel?: string,
    toolVariables?: Array<{ name: string; value: string; type?: string }>
  ) => {
    const isDeleteTool = SEMANTIC_DELETE_TOOL_IDS.includes(toolId);
    if (isDeleteTool) {
      // Use deleteFromDiagram for delete tools — Sirius handles these differently
      deleteMutationFn({
        variables: {
          input: {
            id: crypto.randomUUID(),
            editingContextId,
            representationId,
            nodeIds: diagramElementIds,
            edgeIds: [],
          },
        },
      });
    } else {
      invokeToolMutationFn({
        variables: {
          input: {
            id: crypto.randomUUID(),
            editingContextId,
            representationId,
            toolId,
            diagramElementIds,
            startingPositionX: 200,
            startingPositionY: 200,
            variables: toolVariables || [],
          },
        },
      });
    }
  };

  return [invokeTool, { loading, error: combinedError }];
};
