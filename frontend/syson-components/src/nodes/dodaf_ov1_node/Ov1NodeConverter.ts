import {
  ConnectionHandle, GQLDiagram, GQLDiagramDescription, GQLEdge, GQLHandleLayoutData,
  GQLNode, GQLNodeDescription, GQLNodeLayoutData, GQLNodeStyle, GQLViewModifier,
  IConvertEngine, INodeConverter, convertBorderNodePosition, convertHandles,
  convertInsideLabel, convertLineStyle, convertOutsideLabels, isListLayoutStrategy,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Node, XYPosition } from '@xyflow/react';
import { GQLOv1NodeStyle, Ov1NodeData } from './Ov1Node.types';

const defaultPosition: XYPosition = { x: 0, y: 0 };

const toOv1Node = (
  gqlDiagram: GQLDiagram, gqlNode: GQLNode<GQLOv1NodeStyle>,
  gqlParentNode: GQLNode<GQLNodeStyle> | null, nodeDescription: GQLNodeDescription,
  isBorderNode: boolean, gqlEdges: GQLEdge[]
): Node<Ov1NodeData> => {
  const { targetObjectId, targetObjectLabel, targetObjectKind, descriptionId, id,
    insideLabel, outsideLabels, state, pinned, style, labelEditable, deletable,
    customizedStyleProperties, decorators } = gqlNode;

  const handleLayoutData: GQLHandleLayoutData[] = gqlDiagram.layoutData.nodeLayoutData
    .filter(nld => nld.id === id).flatMap(nld => nld.handleLayoutData);
  const connectionHandles: ConnectionHandle[] = convertHandles(gqlNode.id, gqlEdges, handleLayoutData);
  const gqlNodeLayoutData: GQLNodeLayoutData | undefined = gqlDiagram.layoutData.nodeLayoutData.find(nld => nld.id === id);
  const isNew = gqlNodeLayoutData === undefined;
  const resizedByUser = gqlNodeLayoutData?.resizedByUser ?? false;

  const data: Ov1NodeData = {
    targetObjectId, targetObjectLabel, targetObjectKind, descriptionId,
    style: { display: 'flex', background: style.background, borderColor: style.borderColor,
             borderWidth: style.borderSize, borderStyle: convertLineStyle(style.borderStyle) },
    insideLabel: null, outsideLabels: convertOutsideLabels(outsideLabels, gqlDiagram.layoutData.labelLayoutData),
    faded: state === GQLViewModifier.Faded, pinned, isBorderNode, nodeDescription,
    defaultWidth: gqlNode.defaultWidth, defaultHeight: gqlNode.defaultHeight,
    borderNodePosition: convertBorderNodePosition(gqlNode.initialBorderNodePosition),
    connectionHandles, labelEditable, deletable, isNew, resizedByUser,
    movedByUser: gqlNodeLayoutData?.movedByUser ?? false,
    isListChild: isListLayoutStrategy(gqlParentNode?.style.childrenLayoutStrategy),
    isDraggedNode: false, isDropNodeTarget: false, isDragNodeSource: false, isDropNodeCandidate: false,
    isHovered: false,
    nodeAppearanceData: { gqlStyle: style, customizedStyleProperties },
    connectionLinePositionOnNode: 'none',
    minComputedWidth: gqlNodeLayoutData?.minComputedSize.width ?? null,
    minComputedHeight: gqlNodeLayoutData?.minComputedSize.height ?? null,
    isLastNodeSelected: false, moving: false, decorators,
  };

  data.insideLabel = convertInsideLabel(insideLabel, gqlDiagram.layoutData.labelLayoutData, data,
    `${style.borderSize}px ${style.borderStyle} ${style.borderColor}`);
  if (data.insideLabel) { data.insideLabel.isHeader = true; data.insideLabel.headerPosition = 'TOP'; }

  const node: Node<Ov1NodeData> = {
    id, type: 'dodafOv1Node', data, position: defaultPosition,
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

export class Ov1NodeConverter implements INodeConverter {
  canHandle(gqlNode: GQLNode<GQLNodeStyle>) {
    return gqlNode.style.__typename === 'Ov1NodeStyle';
  }

  handle(convertEngine: IConvertEngine, gqlDiagram: GQLDiagram, gqlNode: GQLNode<GQLOv1NodeStyle>,
         gqlEdges: GQLEdge[], parentNode: GQLNode<GQLNodeStyle> | null, isBorderNode: boolean,
         nodes: Node[], diagramDescription: GQLDiagramDescription, nodeDescriptions: GQLNodeDescription[]) {
    const nd = nodeDescriptions.find(d => d.id === gqlNode.descriptionId);
    if (nd) nodes.push(toOv1Node(gqlDiagram, gqlNode, parentNode, nd, isBorderNode, gqlEdges));
    const borderNDs = (nd?.borderNodeDescriptionIds ?? []).flatMap(id => diagramDescription.nodeDescriptions.filter(d => d.id === id));
    const childNDs = (nd?.childNodeDescriptionIds ?? []).flatMap(id => diagramDescription.nodeDescriptions.filter(d => d.id === id));
    convertEngine.convertNodes(gqlDiagram, gqlNode.borderNodes ?? [], gqlNode, nodes, diagramDescription, borderNDs);
    convertEngine.convertNodes(gqlDiagram, gqlNode.childNodes ?? [], gqlNode, nodes, diagramDescription, childNDs);
  }
}
