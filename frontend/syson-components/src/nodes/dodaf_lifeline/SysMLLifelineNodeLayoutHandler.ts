import {
  Diagram, DiagramNodeType, ForcedDimensions, ILayoutEngine, INodeLayoutHandler, NodeData,
  computePreviousSize, findNodeIndex,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Node } from '@xyflow/react';
import { LifelineNodeData } from './SysMLLifelineNode.types';

export class SysMLLifelineNodeLayoutHandler implements INodeLayoutHandler<LifelineNodeData> {
  public canHandle(node: Node<NodeData, DiagramNodeType>): boolean {
    return node.type === 'dodafLifelineNode';
  }

  public handle(
    _layoutEngine: ILayoutEngine, previousDiagram: Diagram | null,
    node: Node<LifelineNodeData, 'dodafLifelineNode'>, visibleNodes: Node<NodeData, DiagramNodeType>[],
    _directChildren: Node<NodeData, DiagramNodeType>[], _newlyAddedNodes: Node<NodeData, DiagramNodeType>[],
    forceDimensions?: ForcedDimensions
  ) {
    const borderWidth = 2;
    this.handleLeafNode(previousDiagram, node, visibleNodes, borderWidth, forceDimensions);
  }

  private handleLeafNode(
    previousDiagram: Diagram | null, node: Node<LifelineNodeData, 'dodafLifelineNode'>,
    visibleNodes: Node<NodeData, DiagramNodeType>[], borderWidth: number, _forceDimensions?: ForcedDimensions
  ) {
    const nodeIndex: number = findNodeIndex(visibleNodes, node.id);
    const labelElement: HTMLElement | null = document.getElementById(`${node.id}-label-${nodeIndex}`);
    const labelHeight: number = 8 + (labelElement?.getBoundingClientRect().height ?? 0) + 8 + 2 * borderWidth;
    const minNodeWidth: number = (node.data.resizedByUser ? 100 : node.data.defaultWidth ?? 100) + 2 * borderWidth;
    const minNodeHeight: number = Math.max(labelHeight + 120, (node.data.resizedByUser ? 150 : node.data.defaultHeight ?? 150) + 2 * borderWidth);
    // UML lifeline: the dashed line must run down the whole visible diagram, and the
    // hidden connection handles (which define where message edges can snap) are spread
    // as a percentage of the node height. Make the node tall enough so the dashed
    // lifeline (flex:1 inside the node) and its handles cover the visible area.
    const viewportHeight: number = typeof window !== 'undefined' && window.innerHeight > 0 ? window.innerHeight : 600;
    const lifelineHeight: number = Math.max(minNodeHeight, Math.round(viewportHeight * 0.65));
    const previousNode = (previousDiagram?.nodes ?? []).find(pn => pn.id === node.id);
    const previousDimensions = computePreviousSize(previousNode, node);
    if (node.data.resizedByUser && !_forceDimensions?.width) {
      node.width = minNodeWidth > previousDimensions.width ? minNodeWidth : previousDimensions.width;
    } else { node.width = minNodeWidth; }
    if (node.data.resizedByUser && !_forceDimensions?.height) {
      node.height = lifelineHeight > previousDimensions.height ? lifelineHeight : previousDimensions.height;
    } else { node.height = lifelineHeight; }
  }
}
