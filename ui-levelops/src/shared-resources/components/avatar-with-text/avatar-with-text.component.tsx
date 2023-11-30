import React from "react";
import { Typography } from "antd";
import { AntColComponent as AntCol } from "../ant-col/ant-col.component";
import { default as AntRow } from "../ant-row/ant-row.component";
import { NameAvatarComponent as NameAvatar } from "../name-avatar/name-avatar.component";
import "./avatar-with-text.styles.scss";

const { Text } = Typography;

interface AvatarWithTextComponentProps {
  avatarText?: string;
  text: string;
  showTooltip?: boolean;
  className?: string;
}
export const AvatarWithTextComponent: React.FC<AvatarWithTextComponentProps> = (
  props: AvatarWithTextComponentProps
) => {
  return (
    <AntRow gutter={[5, 5]} type={"flex"} align={"middle"} className={props.className}>
      <AntCol>
        <NameAvatar name={props.avatarText || props.text} showTooltip={props.showTooltip || false} />
      </AntCol>
      <AntCol className="display-text">
        <Text ellipsis>{props.text}</Text>
      </AntCol>
    </AntRow>
  );
};
