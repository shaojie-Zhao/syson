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

import { gql, useQuery } from '@apollo/client';
import {
  GQLGetPaletteData,
  GQLGetPaletteVariables,
  GQLPalette,
} from './ToolSidebar.types';

/**
 * GraphQL query to fetch the palette (tool sections + tools) for a given
 * editing context / representation / diagram element combination.
 *
 * When `diagramElementIds` is empty, the query returns the diagram-level palette
 * (the same tools shown when right-clicking on an empty canvas area).
 * When it contains one or more element IDs, it returns the node/edge-specific palette.
 */
export const getPaletteQuery = gql`
  query getPalette($editingContextId: ID!, $representationId: ID!, $diagramElementIds: [ID!]) {
    viewer {
      editingContext(editingContextId: $editingContextId) {
        representation(representationId: $representationId) {
          description {
            ... on DiagramDescription {
              palette(diagramElementIds: $diagramElementIds) {
                id
                quickAccessTools {
                  id
                  label
                  iconURL
                  ... on SingleClickOnDiagramElementTool {
                    dialogDescriptionId
                  }
                }
                paletteEntries {
                  ... on Tool {
                    id
                    label
                    iconURL
                  }
                  ... on SingleClickOnDiagramElementTool {
                    id
                    label
                    iconURL
                    dialogDescriptionId
                  }
                  ... on ToolSection {
                    id
                    label
                    iconURL
                    tools {
                      id
                      label
                      iconURL
                      ... on SingleClickOnDiagramElementTool {
                        dialogDescriptionId
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
`;

/**
 * Hook to fetch palette data (tools organized in sections) for the given
 * editing context, representation, and diagram element IDs.
 *
 * @param editingContextId  The editing context ID.
 * @param representationId  The representation (diagram) ID.
 * @param diagramElementIds The element IDs to get the palette for.
 *                          Pass an empty array to get the diagram-level palette.
 * @returns                 The palette data, loading state, and error.
 */
export const usePalette = (
  editingContextId: string | null,
  representationId: string | null,
  diagramElementIds: string[] | null
): { palette: GQLPalette | null; loading: boolean; error: string | null } => {
  const skip = !editingContextId || !representationId;

  const { data, loading, error } = useQuery<GQLGetPaletteData, GQLGetPaletteVariables>(
    getPaletteQuery,
    {
      variables: {
        editingContextId: editingContextId ?? '',
        representationId: representationId ?? '',
        diagramElementIds: diagramElementIds ?? [],
      },
      skip,
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    }
  );

  if (skip || loading) {
    return { palette: null, loading: true, error: null };
  }

  const palette = data?.viewer?.editingContext?.representation?.description?.palette ?? null;
  return { palette, loading: false, error: error ? error.message : null };
};
