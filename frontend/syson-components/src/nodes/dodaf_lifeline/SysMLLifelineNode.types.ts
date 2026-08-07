import { NodeData } from '@eclipse-sirius/sirius-components-diagrams';
import { Node, NodeProps } from '@xyflow/react';
import { FC } from 'react';

export interface LifelineNodeData extends NodeData {}

export interface NodeDataMap {
  dodafLifelineNode: LifelineNodeData;
}
export type NodeComponentsMap = {
  [K in keyof NodeDataMap]: FC<NodeProps<Node<NodeDataMap[K], K>>>;
};
