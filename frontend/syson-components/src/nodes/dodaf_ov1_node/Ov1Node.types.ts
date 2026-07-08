import { GQLNodeStyle, NodeData } from '@eclipse-sirius/sirius-components-diagrams';
import { Node, NodeProps } from '@xyflow/react';
import { FC } from 'react';

export interface Ov1NodeData extends NodeData {}

export interface GQLOv1NodeStyle extends GQLNodeStyle {
  background: string;
  borderColor: string;
  borderStyle: string;
  borderSize: number;
  iconId: string | null;
}

export interface NodeDataMap {
  dodafOv1Node: Ov1NodeData;
}
export type NodeComponentsMap = {
  [K in keyof NodeDataMap]: FC<NodeProps<Node<NodeDataMap[K], K>>>;
};
