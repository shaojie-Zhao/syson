import {
  Diagram, DiagramNodeType, ForcedDimensions, ILayoutEngine, INodeLayoutHandler, NodeData,
  computePreviousSize, findNodeIndex,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Node } from '@xyflow/react';
import { Ov1NodeData } from './Ov1Node.types';

const rectangularNodePadding: number = 8;

export class Ov1NodeLayoutHandler implements INodeLayoutHandler<Ov1NodeData> {
  public canHandle(node: Node<NodeData, DiagramNodeType>): boolean {
    return node.type === 'dodafOv1Node';
  }

  public handle(
    _layoutEngine: ILayoutEngine, previousDiagram: Diagram | null,
    node: Node<Ov1NodeData, 'dodafOv1Node'>, visibleNodes: Node<NodeData, DiagramNodeType>[],
    _directChildren: Node<NodeData, DiagramNodeType>[], _newlyAddedNodes: Node<NodeData, DiagramNodeType>[],
    forceDimensions?: ForcedDimensions
  ) {
    const borderWidth = 2;
    this.handleLeafNode(previousDiagram, node, visibleNodes, borderWidth, forceDimensions);
  }

  private handleLeafNode(
    previousDiagram: Diagram | null, node: Node<Ov1NodeData, 'dodafOv1Node'>,
    visibleNodes: Node<NodeData, DiagramNodeType>[], borderWidth: number, _forceDimensions?: ForcedDimensions
  ) {
    const nodeIndex: number = findNodeIndex(visibleNodes, node.id);
    const labelElement: HTMLElement | null = document.getElementById(`${node.id}-label-${nodeIndex}`);
    const labelHeight: number = rectangularNodePadding + (labelElement?.getBoundingClientRect().height ?? 0) + rectangularNodePadding + 2 * borderWidth;
    const minNodeWidth: number = (node.data.resizedByUser ? 75 : node.data.defaultWidth ?? 75) + 2 * borderWidth;
    const minNodeHeight: number = Math.max(labelHeight, (node.data.resizedByUser ? 50 : node.data.defaultHeight ?? 50) + 2 * borderWidth);
    const previousNode = (previousDiagram?.nodes ?? []).find(pn => pn.id === node.id);
    const previousDimensions = computePreviousSize(previousNode, node);
    if (node.data.resizedByUser && !_forceDimensions?.width) {
      node.width = minNodeWidth > previousDimensions.width ? minNodeWidth : previousDimensions.width;
    } else { node.width = minNodeWidth; }
    if (node.data.resizedByUser && !_forceDimensions?.height) {
      node.height = minNodeHeight > previousDimensions.height ? minNodeHeight : previousDimensions.height;
    } else { node.height = minNodeHeight; }
  }
}
