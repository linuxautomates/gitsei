import React, { CSSProperties, useMemo } from "react";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { default as AntIcon } from "../ant-icon/ant-icon.component";
import { Popover } from "antd";
import "./title-with-info.scss";

interface TitleWithInfoProps {
  title: string;
  titleStyle?: CSSProperties;
  tooltipHeader?: string;
  description: string;
  style?: any;
  className?: string;
}

const TitleWithInfo: React.FC<TitleWithInfoProps> = (props: TitleWithInfoProps) => {
  const { description, titleStyle, style, className } = props;

  const mappedDescription = useMemo(() => {
    const _chunks = description.split("\n");
    const _spans: JSX.Element[] = [];
    _chunks.forEach(chunk => _spans.push(<span key={`chunk-${chunk}`}>{chunk}</span>));
    return <div className="flex direction-column">{_spans}</div>;
  }, [description]);

  return (
    <div style={style} className={`centered titleWithInfo ${className}`}>
      <AntText style={titleStyle || {}}>{props.title}</AntText>
      <Popover content={mappedDescription} trigger="hover">
        <div className="info-icon">
          <AntIcon type="info-circle" />
        </div>
      </Popover>
    </div>
  );
};

export default TitleWithInfo;
