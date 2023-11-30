import { Avatar, Tooltip } from "antd";
import { map } from "lodash";
import React, { useMemo } from "react";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { collabStateColorMapping } from "./constant";
import { getFirstAndLastChildWithNonZeroVal } from "./scm-review-collaboration.helper";

const alphaVal = (s: string) => (s || "").toLowerCase().charCodeAt(0) - 97;

const calculatePercent = (total: number, acc: number) => ((total - acc) / total) * 100;

interface CollabItemProps {
  index: number;
  name: string;
  total_prs: number;
  collabOnStates: string[];
  unapproved: number;
  self_approved: number;
  self_approved_with_review: number;
  unassigned_peer_approved: number;
  assigned_peer_approved: number;
  reviewerPRsPercent?: string | number;
  maxValue: number;
  overallPrs?: number;
  isStart: boolean;
}

const avatar = (name: string) => (
  <Avatar key={name} size="small" className={`background-color-${(alphaVal(name) % 5) + 1}`}>
    {(name?.substring(0, 2) || "").toUpperCase()}
  </Avatar>
);

const CollabItem: React.FC<CollabItemProps> = (props: CollabItemProps) => {
  const {
    name,
    total_prs,
    isStart,
    maxValue,
    unapproved,
    self_approved,
    self_approved_with_review,
    unassigned_peer_approved,
    assigned_peer_approved,
    collabOnStates,
    overallPrs,
    reviewerPRsPercent
  } = props;

  const percent = 100 - calculatePercent(maxValue, total_prs);
  const record: any = {
    unapproved,
    self_approved,
    self_approved_with_review,
    unassigned_peer_approved,
    assigned_peer_approved
  };

  const [firstKey, lastKey] = getFirstAndLastChildWithNonZeroVal(record, collabOnStates);

  const moreStyle = useMemo(() => {
    if (isStart) {
      return {
        backgroundColor: "#E5E5E5",
        boxShadow: `0px 9px 28px 8px rgba(0, 0, 0, 0.05), 0px 6px 16px rgba(0, 0, 0, 0.08), 0px 3px 6px -4px rgba(0, 0, 0, 0.12)`
      };
    }
    return {};
  }, [isStart]);

  return (
    <div
      className={"w-100 flex align-center justify-space-between"}
      style={{
        height: "2.7rem",
        borderRadius: 100,
        margin: "0.5rem 0",
        ...moreStyle
      }}>
      <div
        style={{
          borderTopLeftRadius: 100,
          borderBottomLeftRadius: 100,
          whiteSpace: "nowrap",
          overflow: "hidden",
          textOverflow: "ellipsis"
        }}
        className={"w-25 h-100"}>
        <Tooltip title={name}>
          <div className={"flex align-center flex justify-start align-center h-100 p-10"}>
            <div className={"mr-10"}>{avatar(name)}</div>
            <span style={{ color: "#2967DD", fontSize: 14, textOverflow: "ellipsis", overflow: "hidden" }}>{name}</span>
          </div>
        </Tooltip>
      </div>
      <div className={"flex justify-start mr-20 ml-35 flex-1"}>
        <div style={{ width: `${percent}%` }} className="review-bar">
          {map(collabOnStates, key => {
            return (
              <div
                style={{
                  width: `${record[key]}%`,
                  backgroundColor: (collabStateColorMapping as any)[key],
                  height: "40%",
                  borderRadius:
                    firstKey === lastKey
                      ? "100px 100px 100px 100px"
                      : firstKey === key
                      ? "100px 0 0 100px"
                      : lastKey === key
                      ? "0 100px 100px 0"
                      : "0 0 0 0"
                }}
              />
            );
          })}
        </div>
      </div>
      <div
        className={"h-100"}
        style={{
          borderTopRightRadius: 100,
          borderBottomRightRadius: 100
        }}>
        <div className={"flex justify-end align-center h-100 py-5 pr-15 pl-15"}>
          <SvgIconComponent className="flex justify-center align-center" icon={"branch"} style={{ height: "17px" }} />
          <span style={{ fontSize: 14, marginLeft: 5 }}>{overallPrs ? `${total_prs}/${overallPrs}` : total_prs}</span>
          {!!reviewerPRsPercent && (
            <div className="reviewer-to-committer-percent">
              <span>{`${reviewerPRsPercent}%`}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default React.memo(CollabItem);
