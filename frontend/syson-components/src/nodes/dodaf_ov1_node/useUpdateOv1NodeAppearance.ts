import { gql, useMutation } from '@apollo/client';
import { useReporting } from '@eclipse-sirius/sirius-components-core';

const editOv1NodeAppearanceMutation = gql`
  mutation editOv1NodeAppearance($input: EditOv1NodeAppearanceInput!) {
    editOv1NodeAppearance(input: $input) {
      __typename
      ... on ErrorPayload { message }
      ... on SuccessPayload { messages { level body } }
    }
  }
`;

export const useUpdateOv1NodeAppearance = (editingContextId: string, representationId: string) => {
  const [mutation, result] = useMutation(editOv1NodeAppearanceMutation);
  useReporting(result, (data) => data.editOv1NodeAppearance);
  const editAppearance = (nodeIds: string[], appearance: { background?: string; borderColor?: string; borderSize?: number; borderStyle?: string; iconId?: string | null }) => {
    mutation({ variables: { input: { id: crypto.randomUUID(), editingContextId, representationId, nodeIds, appearance } } });
  };
  return { editAppearance };
};
