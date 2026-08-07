import {
  ConnectionHandle, GQLDiagram, GQLDiagramDescription, GQLEdge, GQLHandleLayoutData,
  GQLNode, GQLNodeDescription, GQLNodeLayoutData, GQLNodeStyle,
  GQLViewModifier, IConvertEngine, INodeConverter, convertBorderNodePosition, convertHandles,
  convertInsideLabel, convertLineStyle, convertOutsideLabels, isListLayoutStrategy,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Node, XYPosition } from '@xyflow/react';
import { LifelineNodeData } from './SysMLLifelineNode.types';

// The package does not re-export GQLRectangularNodeStyle; only the fields we need
// are declared locally so the border properties can be read from the node style.
interface RectangularNodeStyleFields {
  borderColor: string;
  borderSize: number;
  borderStyle: string;
}

const defaultPosition: XYPosition = { x: 0, y: 0 };

const toLifelineNode = (
  gqlDiagram: GQLDiagram, gqlNode: GQLNode<GQLNodeStyle>,
  gqlParentNode: GQLNode<GQLNodeStyle> | null, nodeDescription: GQLNodeDescription,
  isBorderNode: boolean, gqlEdges: GQLEdge[]
): Node<LifelineNodeData> => {
  const { targetObjectId, targetObjectLabel, targetObjectKind, descriptionId, id,
    insideLabel, outsideLabels, state, pinned, style, labelEditable, deletable, decorators } = gqlNode;
  // Lifeline nodes are rendered as RectangularNodeStyle; narrow the style type so the
  // border properties can be read safely.
  const rectStyle = style as unknown as RectangularNodeStyleFields;

  const handleLayoutData: GQLHandleLayoutData[] = gqlDiagram.layoutData.nodeLayoutData
    .filter(nld => nld.id === id).flatMap(nld => nld.handleLayoutData);
  const connectionHandles: ConnectionHandle[] = convertHandles(gqlNode.id, gqlEdges, handleLayoutData);
  const gqlNodeLayoutData: GQLNodeLayoutData | undefined = gqlDiagram.layoutData.nodeLayoutData.find(nld => nld.id === id);
  const isNew = gqlNodeLayoutData === undefined;
  const resizedByUser = gqlNodeLayoutData?.resizedByUser ?? false;

  const data: LifelineNodeData = {
    targetObjectId, targetObjectLabel, targetObjectKind, descriptionId,
    nodeAppearanceData: { customizedStyleProperties: gqlNode.customizedStyleProperties, gqlStyle: style },
    style: { display: 'flex', background: 'transparent', borderColor: rectStyle.borderColor,
             borderWidth: rectStyle.borderSize, borderStyle: convertLineStyle(rectStyle.borderStyle) },
    insideLabel: null, outsideLabels: convertOutsideLabels(outsideLabels, gqlDiagram.layoutData.labelLayoutData),
    faded: state === GQLViewModifier.Faded, pinned, isBorderNode, nodeDescription,
    defaultWidth: gqlNode.defaultWidth, defaultHeight: gqlNode.defaultHeight,
    borderNodePosition: convertBorderNodePosition(gqlNode.initialBorderNodePosition),
    connectionHandles, labelEditable, deletable, isNew, resizedByUser,
    movedByUser: gqlNodeLayoutData?.movedByUser ?? false,
    isListChild: isListLayoutStrategy(gqlParentNode?.style.childrenLayoutStrategy),
    isDraggedNode: false, isDropNodeTarget: false, isDragNodeSource: false, isDropNodeCandidate: false,
    isHovered: false,
    connectionLinePositionOnNode: 'none',
    minComputedWidth: gqlNodeLayoutData?.minComputedSize.width ?? null,
    minComputedHeight: gqlNodeLayoutData?.minComputedSize.height ?? null,
    isLastNodeSelected: false, moving: false, decorators,
  };

  data.insideLabel = convertInsideLabel(insideLabel, gqlDiagram.layoutData.labelLayoutData, data,
    `${rectStyle.borderSize}px ${rectStyle.borderStyle} ${rectStyle.borderColor}`);

  const node: Node<LifelineNodeData> = {
    id, type: 'dodafLifelineNode', data, position: defaultPosition,
    hidden: gqlNode.state === GQLViewModifier.Hidden,
  };
  if (gqlParentNode) node.parentId = gqlParentNode.id;

  const nodeLayoutData = gqlDiagram.layoutData.nodeLayoutData.find(d => d.id === id);
  if (nodeLayoutData) {
    node.position = nodeLayoutData.position;
    node.height = nodeLayoutData.size.height;
    node.width = nodeLayoutData.size.width;
    node.style = { ...node.style, width: `${node.width}px`, height: `${node.height}px` };
    node.measured = { height: nodeLayoutData.size.height, width: nodeLayoutData.size.width };
  }
  return node;
};

export class SysMLLifelineNodeConverter implements INodeConverter {
  canHandle(gqlNode: GQLNode<GQLNodeStyle>) {
    // Intercept Lifeline nodes rendered as list items (RectangularNodeStyle + List
    // layout strategy) as well as any other style, as long as the label contains
    // "Lifeline".
    return (gqlNode.insideLabel?.text?.includes('Lifeline') ?? false);
  }

  handle(_convertEngine: IConvertEngine, gqlDiagram: GQLDiagram, gqlNode: GQLNode<GQLNodeStyle>,
         gqlEdges: GQLEdge[], parentNode: GQLNode<GQLNodeStyle> | null, isBorderNode: boolean,
         nodes: Node[], _diagramDescription: GQLDiagramDescription, nodeDescriptions: GQLNodeDescription[]) {
    const nd = nodeDescriptions.find(d => d.id === gqlNode.descriptionId);
    if (nd) nodes.push(toLifelineNode(gqlDiagram, gqlNode, parentNode, nd, isBorderNode, gqlEdges));
    return nodes;
  }
}
