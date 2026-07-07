import { getCSSColor } from '@eclipse-sirius/sirius-components-core';
import {
  ConnectionCreationHandles, ConnectionHandles, ConnectionTargetHandle,
  DecoratorContainer, Label, Resizer, useConnectionLineNodeStyle,
  useConnectorNodeStyle, useDrop, useDropNodeStyle, useRefreshConnectionHandles,
} from '@eclipse-sirius/sirius-components-diagrams';
import { Theme, useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import { Node, NodeProps } from '@xyflow/react';
import React, { memo } from 'react';
import { NodeComponentsMap, Ov1NodeData } from './Ov1Node.types';

const ov1NodeStyle = (theme: Theme, style: React.CSSProperties, selected: boolean, hovered: boolean, faded: boolean): React.CSSProperties => ({
  display: 'flex', padding: '0px', width: '100%', height: '100%', position: 'relative',
  opacity: faded ? '0.4' : '', ...style, border: 'none', backgroundColor: 'transparent',
  ...(selected || hovered ? { outline: `${theme.palette.selected} solid 1px` } : {}),
});

const headerStyle = (theme: Theme, style: React.CSSProperties, selected: boolean, hovered: boolean, faded: boolean): React.CSSProperties => ({
  display: 'inline-flex', flexDirection: 'row', flexWrap: 'nowrap', padding: '0 16px 0 0',
  width: 'fit-content', maxWidth: '70%', opacity: faded ? '0.4' : '',
  ...style, background: getCSSColor(String(style.background), theme),
  borderStyle: 'dashed dashed none',
  borderRightColor: getCSSColor(String(style.borderColor), theme),
  borderLeftColor: getCSSColor(String(style.borderColor), theme),
  borderTopColor: getCSSColor(String(style.borderColor), theme),
  overflowX: 'clip',
  ...(selected || hovered ? { outline: `${theme.palette.selected} solid 1px` } : {}),
});

const containerStyle = (theme: Theme, style: React.CSSProperties, selected: boolean, hovered: boolean, faded: boolean): React.CSSProperties => ({
  display: 'flex', padding: '2px', width: '100%', height: '100%', opacity: faded ? '0.4' : '',
  ...style, background: getCSSColor(String(style.background), theme),
  borderColor: getCSSColor(String(style.borderColor), theme),
  borderStyle: 'solid', alignItems: 'center', justifyContent: 'center',
  ...(selected || hovered ? { outline: `${theme.palette.selected} solid 1px` } : {}),
});

export const Ov1Node: NodeComponentsMap['dodafOv1Node'] = memo(
  ({ data, id, selected, dragging }: NodeProps<Node<Ov1NodeData>>) => {
    const theme: Theme = useTheme();
    const { onDrop, onDragOver } = useDrop();
    const { style: connectionFeedbackStyle } = useConnectorNodeStyle(id, data.nodeDescription.id);
    const { style: dropFeedbackStyle } = useDropNodeStyle(data.isDropNodeTarget, data.isDragNodeSource, data.isDropNodeCandidate, dragging);
    const { style: connectionLineActiveNodeStyle } = useConnectionLineNodeStyle(data.connectionLinePositionOnNode);
    useRefreshConnectionHandles(id, data.connectionHandles);

    const label: any = { ...data.insideLabel, style: { ...data?.insideLabel?.style, overflow: 'hidden', paddingRight: '0', justifyContent: 'flex-start', textAlign: 'left' } };

    const visibilityPart = () => {
      if (data.insideLabel != null) {
        const idx = data.insideLabel.text.indexOf('»');
        if (idx > -1) return data.insideLabel.text.substring(0, idx + 1);
      }
      return '';
    };
    const labelText = () => {
      if (data.insideLabel != null) {
        const idx = data.insideLabel.text.indexOf('»');
        return idx > -1 ? data.insideLabel.text.substring(idx + 2) : data.insideLabel.text;
      }
      return '';
    };
    const splitLabel = { ...label, text: labelText(), iconURL: [], style: { ...label.style, paddingLeft: '0' } };

    // Icon rendering from Ov1NodeStyle.iconId
    const gqlStyle = (data.nodeAppearanceData as any)?.gqlStyle;
    const iconId: string | null = gqlStyle?.iconId ?? null;

    const labelElement = data.insideLabel && (
      <>
        <Typography color={'black'} data-svg="text">{visibilityPart()}</Typography>
        <Label diagramElementId={id} label={splitLabel} faded={data.faded} />
      </>
    );

    return (
      <>
        <Resizer data={data} selected={!!selected} />
        <div style={{ ...ov1NodeStyle(theme, data.style, !!selected, data.isHovered, data.faded) }}
             onDragOver={onDragOver} onDrop={(e) => onDrop(e, id)}
             data-testid={`OV1Node - ${data?.insideLabel?.text}`}>
          <DecoratorContainer decorators={data.decorators}></DecoratorContainer>
          {!!selected ? <ConnectionCreationHandles nodeId={id} /> : null}
          <ConnectionTargetHandle nodeId={id} nodeDescription={data.nodeDescription} isHovered={data.isHovered} />
          <ConnectionHandles connectionHandles={data.connectionHandles} />
          <div style={{ ...headerStyle(theme, data.style, !!selected, data.isHovered, data.faded), ...connectionFeedbackStyle, ...dropFeedbackStyle, ...connectionLineActiveNodeStyle }} data-svg="rect">
            {data.insideLabel ? <Label diagramElementId={id} label={{ ...label, text: '  ' }} faded={data.faded} /> : null}
          </div>
          <div style={{ ...containerStyle(theme, data.style, !!selected, data.isHovered, data.faded), ...connectionFeedbackStyle, ...dropFeedbackStyle, ...connectionLineActiveNodeStyle, flexDirection: 'column' }} data-svg="rect">
            {iconId && (
              <img src={iconId} alt="icon" style={{ width: 32, height: 32, marginBottom: 4 }} />
            )}
            {labelElement}
          </div>
        </div>
      </>
    );
  }
);
